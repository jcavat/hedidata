package com.github.hedidata

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging

import scala.concurrent.duration._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.model.headers.HttpOriginRange
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings

import scala.concurrent.Future
import com.github.hedidata.repository.MongoRepositoryActor._
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.mongodb.scala.bson.ObjectId

import collection.JavaConverters._
import scala.util.{ Failure, Success }

trait ServerRoutes extends JsonSupport {

  import domain.Entities._

  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[ServerRoutes])

  def userRegistryActor: ActorRef

  implicit lazy val timeout = Timeout(5.seconds)

  val tokens: List[String] = ConfigFactory.load().getStringList("tokens").asScala.toList

  val corsSettings: CorsSettings = CorsSettings.defaultSettings.withAllowedOrigins(HttpOriginRange.*)

  def check(credentials: Credentials): Option[String] = credentials match {
    case p @ Credentials.Provided(token) if tokens.exists(t => p.verify(t)) => Some(token)
    case _ => None
  }

  val therapistsDirective = pathPrefix("therapists") {
      pathEnd {
        concat(
          post {
            entity(as[Therapist]) { user =>
              val newUser = if (user._id.isEmpty) user.copy(_id = Some(new ObjectId())) else user

              val userCreated: Future[TherapistCreated] =
                (userRegistryActor ? CreateTherapist(newUser)).mapTo[TherapistCreated]

              onComplete(userCreated) {
                case Success(objectId) => complete((StatusCodes.Created, objectId))
                case Failure(e) => complete(HttpResponse(StatusCodes.InternalServerError, entity = e.toString))
              }
            }
          },
          get {
            import spray.json.DefaultJsonProtocol._
            val users: Future[Seq[Therapist]] =
              (userRegistryActor ? GetTherapists).mapTo[Seq[Therapist]]
            rejectEmptyResponse {
              complete(users)
            }
          })
      }
    }

  lazy val allRoutes: Route = cors(corsSettings) {
    authenticateOAuth2(realm = "secure site", check) { token =>
      therapistsDirective
    }
  }
}
