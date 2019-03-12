package com.github.hedidata.route

import java.util.concurrent.TimeUnit

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ Directive1, Route }
import authentikat.jwt.{ JsonWebToken, JwtClaimsSet, JwtHeader }
import com.github.hedidata.JsonSupport

object LoginRoutesWithAuth {
  final case class LoginRequest(username: String, password: String)
  private val tokenExpiryPeriodInDays = 1
  private val secretKey = "ABCDEFGH"
  private val header = JwtHeader("HS256")
}

trait LoginRoutesWithAuth extends JsonSupport {

  // based on: https://blog.codecentric.de/en/2017/09/jwt-authentication-akka-http/

  import LoginRoutesWithAuth._

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

  def authenticated: Directive1[Map[String, Any]] =
    optionalHeaderValueByName("Authorization").flatMap {
      case Some(jwt) if isTokenExpired(jwt) =>
        complete(StatusCodes.Unauthorized -> "Token expired.")

      case Some(jwt) if JsonWebToken.validate(jwt, secretKey) =>
        provide(getClaims(jwt).getOrElse(Map.empty[String, Any]))

      case _ => complete(StatusCodes.Unauthorized)
    }

  def login: Route = post {
    entity(as[LoginRequest]) {
      // TODO: repository for login/password
      case lr @ LoginRequest("admin", "admin") =>
        val claims = grantClaims(lr.username, tokenExpiryPeriodInDays)
        respondWithHeader(RawHeader("Access-Token", JsonWebToken(header, claims, secretKey))) {
          complete(StatusCodes.OK)
        }
      case LoginRequest(_, _) => complete(StatusCodes.Unauthorized)
    }
  }
}
