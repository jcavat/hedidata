package com.github.hedidata
package repository

import akka.actor.{ Actor, ActorLogging, ActorRef, Props, Status }
import akka.pattern.pipe
import com.github.hedidata.route.ConsultationDto
import org.bson.codecs.configuration.CodecRegistries.{ fromProviders, fromRegistries }
import org.bson.codecs.configuration.CodecRegistry
import org.bson.types.ObjectId
import org.mongodb.scala._
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor, Future }
import domain.Entities.{ Consultation, Entity, Patient, Therapist }

import scala.util.{ Failure, Success }

object MongoRepositoryActor {
  // Commands
  final case object GetTherapists
  final case object GetPatients
  final case class GetConsultations(patientId: ObjectId)
  final case class Create(entity: Entity)
  final case class AddConsultation(idPatient: ObjectId, consultation: Consultation)

  // Event
  final case class ResourceCreated(id: ObjectId)
  final case class ConsultationAdded()

  def props(login: String, password: String): Props = Props(new MongoRepositoryActor(login, password))
}

class MongoRepositoryActor(login: String, password: String) extends Actor with ActorLogging {

  import MongoRepositoryActor._
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  val codecRegistry: CodecRegistry =
    fromRegistries(
      fromProviders(
        classOf[Therapist],
        classOf[Patient],
        classOf[Consultation]), DEFAULT_CODEC_REGISTRY)

  val mongoClient: MongoClient = MongoClient(s"mongodb://$login:$password@localhost")
  val database: MongoDatabase = mongoClient.getDatabase("db-hedidata").withCodecRegistry(codecRegistry)
  val therapistCollection: MongoCollection[Therapist] = database.getCollection("therapists")
  val patientCollection: MongoCollection[Patient] = database.getCollection("patients")

  def insertObserver(sender: ActorRef, objectId: ObjectId): Observer[Completed] = new Observer[Completed] {
    override def onNext(result: Completed): Unit = println(s"onNext: $result")
    override def onError(e: Throwable): Unit = println(s"onError: $e")
    override def onComplete(): Unit = { println("onComplete"); sender ! ResourceCreated(objectId) }
  }

  import org.mongodb.scala.model.Filters._
  import org.mongodb.scala.model.Updates._

  def receive: Receive = {

    case GetTherapists =>
      therapistCollection.find().toFuture() pipeTo sender()

    case GetPatients =>
      patientCollection.find().toFuture() pipeTo sender()

    case GetConsultations(idPatient) =>
      val s = sender()
      patientCollection.find(equal("_id", idPatient)).first().toFuture().onComplete {
        case Success(patient) => s ! patient.consultations
        case Failure(e) => s ! e
      }

    case Create(entity) => entity match {
      case patient: Patient =>
        patientCollection.insertOne(patient).subscribe(insertObserver(sender(), patient._id.get))
      case therapist: Therapist =>
        therapistCollection.insertOne(therapist).subscribe(insertObserver(sender(), therapist._id.get))
    }

    case AddConsultation(idPatient, consultation) =>

      val relatedTherapist: Future[Therapist] = therapistCollection.find(equal("_id", consultation.idTherapist)).first().toFuture()
      val relatedPatient: Future[Patient] = patientCollection.find(equal("_id", idPatient)).first().toFuture()

      val checkExistence: Future[Unit] = for {
        therapist <- relatedTherapist
        patient <- relatedPatient
      } yield {
        if (therapist == null) throw new Exception("Therapist not found")
        if (patient == null) throw new Exception("Patient not found")
        ()
      }

      val updatedPatient: Future[Patient] = checkExistence.flatMap(_ =>
        patientCollection
          .findOneAndUpdate(
            equal("_id", idPatient),
            push("consultations", consultation)).toFuture())

      updatedPatient.map(_ => ConsultationAdded()) pipeTo sender()

  }
}
