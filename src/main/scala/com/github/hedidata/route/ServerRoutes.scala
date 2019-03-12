package com.github.hedidata.route

import java.util.concurrent.TimeUnit

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging
import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.model.headers.{ HttpOriginRange, RawHeader }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ Directive1, ExceptionHandler, Route }
import akka.http.scaladsl.server.directives.Credentials
import akka.util.Timeout
import authentikat.jwt.{ JsonWebToken, JwtClaimsSet, JwtHeader }
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import com.github.hedidata.{ JsonSupport, domain }
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters._
import scala.concurrent.duration._

object ServerRoutes {
  final case class LoginRequest(username: String, password: String)
  private val tokenExpiryPeriodInDays = 1
  private val secretKey = "ABCDEFGH"
  private val header = JwtHeader("HS256")
}

trait ServerRoutes extends JsonSupport with TherapistsRoutes with PatientsRoutes with ConsultationsRoutes {

  import ServerRoutes._

  implicit def system: ActorSystem
  lazy val log = Logging(system, classOf[ServerRoutes])
  implicit lazy val timeout = Timeout(5.seconds)

  def repositoryActor: ActorRef

  val tokens: List[String] = ConfigFactory.load().getStringList("tokens").asScala.toList
  val corsSettings: CorsSettings = CorsSettings.defaultSettings.withAllowedOrigins(HttpOriginRange.*)

  def objectIdExceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case _: IllegalArgumentException =>
        extractUri { id =>
          println(s"$id is malformated")
          complete(HttpResponse(StatusCodes.BadRequest, entity = s"$id is malformated and does not conform to an ObjectId"))
        }
    }

  def check(credentials: Credentials): Option[String] = credentials match {
    case p @ Credentials.Provided(token) if tokens.exists(t => p.verify(t)) => Some(token)
    case _ => None
  }

  private def grantClaims(username: String, expiryPeriodInDays: Long) = JwtClaimsSet(
    Map(
      "user" -> username,
      "expiredAt" -> (System.currentTimeMillis() + TimeUnit.DAYS.toMillis(expiryPeriodInDays))))

  private def isTokenExpired(jwt: String) = getClaims(jwt) match {
    case Some(claims) =>
      claims.get("expiredAt") match {
        case Some(value) => value.toLong < System.currentTimeMillis()
        case None => false
      }
    case None => false
  }

  private def getClaims(jwt: String) = jwt match {
    case JsonWebToken(_, claims, _) => claims.asSimpleMap.toOption
    case _ => None
  }

  private def authenticated: Directive1[Map[String, Any]] =
    optionalHeaderValueByName("Authorization").flatMap {
      case Some(jwt) if isTokenExpired(jwt) =>
        complete(StatusCodes.Unauthorized -> "Token expired.")

      case Some(jwt) if JsonWebToken.validate(jwt, secretKey) =>
        provide(getClaims(jwt).getOrElse(Map.empty[String, Any]))

      case _ => complete(StatusCodes.Unauthorized)
    }

  private def login: Route = post {
    entity(as[LoginRequest]) {
      case lr @ LoginRequest("admin", "admin") =>
        val claims = grantClaims(lr.username, tokenExpiryPeriodInDays)
        respondWithHeader(RawHeader("Access-Token", JsonWebToken(header, claims, secretKey))) {
          complete(StatusCodes.OK)
        }
      case LoginRequest(_, _) => complete(StatusCodes.Unauthorized)
    }
  }

  lazy val allRoutes: Route = cors(corsSettings) {
    login ~ authenticated { claims =>
      therapistsDirective ~ patientDirective ~ consultationsDirective
    }
  }
}
