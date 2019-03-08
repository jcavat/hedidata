package com.github.hedidata
package repository

import akka.actor.{ Actor, ActorLogging, Props }
import akka.pattern.pipe
import org.bson.codecs.configuration.CodecRegistries.{ fromProviders, fromRegistries }
import org.bson.codecs.configuration.CodecRegistry
import org.bson.types.ObjectId
import org.mongodb.scala._
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor }
import domain.Entities.{ Consultation, Patient, Therapist }

object MongoRepositoryActor {
  // Commands
  final case object GetTherapists
  final case class CreateTherapist(therapist: Therapist)

  // Event
  final case class TherapistCreated(id: ObjectId)

  def props(login: String, password: String): Props = Props(new MongoRepositoryActor(login, password))
}

class MongoRepositoryActor(login: String, password: String) extends Actor with ActorLogging {

  import MongoRepositoryActor._
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  val codecRegistry: CodecRegistry = fromRegistries(fromProviders(classOf[Therapist], classOf[Patient], classOf[Consultation]), DEFAULT_CODEC_REGISTRY)

  val mongoClient: MongoClient = MongoClient(s"mongodb://$login:$password@localhost")
  val database: MongoDatabase = mongoClient.getDatabase("db-hedidata").withCodecRegistry(codecRegistry)
  val therapistCollection: MongoCollection[Therapist] = database.getCollection("therapists")

  def receive: Receive = {
    case GetTherapists =>
      therapistCollection.find().toFuture() pipeTo sender()

    case CreateTherapist(therapist) =>
      therapistCollection.insertOne(therapist).subscribe(new Observer[Completed] {
        override def onNext(result: Completed): Unit = println(s"onNext: $result")
        override def onError(e: Throwable): Unit = println(s"onError: $e")
        override def onComplete(): Unit = println("onComplete")
      })
      sender() ! TherapistCreated(therapist._id.get)
  }
}
