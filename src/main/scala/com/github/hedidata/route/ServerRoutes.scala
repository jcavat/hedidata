package com.github.hedidata.route

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging
import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.model.headers.HttpOriginRange
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ ExceptionHandler, Route }
import akka.http.scaladsl.server.directives.Credentials
import akka.util.Timeout
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import com.github.hedidata.{ JsonSupport, domain }
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters._
import scala.concurrent.duration._

trait ServerRoutes extends JsonSupport with TherapistsRoutes with PatientsRoutes with ConsultationsRoutes {

  def objectIdExceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case _: IllegalArgumentException =>
        extractUri { id =>
          println(s"$id is malformated")
          complete(HttpResponse(StatusCodes.BadRequest, entity = s"$id is malformated and does not conform to an ObjectId"))
        }
    }

  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[ServerRoutes])

  def repositoryActor: ActorRef

  implicit lazy val timeout = Timeout(5.seconds)

  val tokens: List[String] = ConfigFactory.load().getStringList("tokens").asScala.toList

  val corsSettings: CorsSettings = CorsSettings.defaultSettings.withAllowedOrigins(HttpOriginRange.*)

  def check(credentials: Credentials): Option[String] = credentials match {
    case p @ Credentials.Provided(token) if tokens.exists(t => p.verify(t)) => Some(token)
    case _ => None
  }

  lazy val allRoutes: Route = cors(corsSettings) {
    authenticateOAuth2(realm = "secure site", check) { token =>
      therapistsDirective ~ patientDirective ~ consultationsDirective
    }
  }
}
