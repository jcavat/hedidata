package com.github.hedidata
package repository

import akka.actor.{ Actor, ActorLogging, ActorRef, Props, Status }
import akka.pattern.pipe
import org.bson.codecs.configuration.CodecRegistries.{ fromProviders, fromRegistries }
import org.bson.codecs.configuration.CodecRegistry
import org.bson.types.ObjectId
import org.mongodb.scala._
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor }
import domain.Entities.{ Consultation, Entity, Patient, Therapist }

import scala.util.{ Failure, Success }

final case class ConsultationDto(idPatient: ObjectId, idTherapist: ObjectId, resume: String)

object MongoRepositoryActor {
  // Commands
  final case object GetTherapists
  final case class Create(entity: Entity)
  final case class AddConsultation(consultationDto: ConsultationDto)

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

  def receive: Receive = {

    case GetTherapists =>
      therapistCollection.find().toFuture() pipeTo sender()

    case Create(entity) => entity match {
      case patient: Patient =>
        patientCollection.insertOne(patient).subscribe(insertObserver(sender(), patient._id.get))
      case therapist: Therapist =>
        therapistCollection.insertOne(therapist).subscribe(insertObserver(sender(), therapist._id.get))
    }

    case AddConsultation(consultationDto) =>
      import org.mongodb.scala.model.Filters._
      import org.mongodb.scala.model.Updates._
      val s = sender()
      therapistCollection.find(equal("_id", consultationDto.idTherapist)).limit(1).toFuture().onComplete {
        case Success(therapists) if therapists.nonEmpty => {
          patientCollection
            .findOneAndUpdate(
              equal("_id", consultationDto.idPatient),
              push("consultations", Consultation(consultationDto.idTherapist, consultationDto.resume)))
            .subscribe(new Observer[Patient] {
              override def onNext(result: Patient): Unit = println(result)
              override def onError(e: Throwable): Unit = println(e)
              override def onComplete(): Unit = s ! ConsultationAdded()
            })
        }
        case _ => s ! Status.Failure(new Exception("Therapist not found"))
      }
  }
}
