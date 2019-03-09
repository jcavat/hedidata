package com.github.hedidata.route

import akka.actor.ActorRef
import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives.{ entity, _ }
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.util.Timeout
import com.github.hedidata.repository.MongoRepositoryActor._
import org.mongodb.scala.bson.ObjectId
import com.github.hedidata.{ JsonSupport, domain }

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

final case class ConsultationDto(idPatient: ObjectId, idTherapist: ObjectId, resume: String)

trait ConsultationsRoutes extends JsonSupport {

  import domain.Entities._

  def repositoryActor: ActorRef

  private implicit lazy val timeout = Timeout(5.seconds)

  val consultationsDirective = pathPrefix("consultations") {
    pathEnd {
      concat(
        post {
          entity(as[ConsultationDto]) { consultationDto =>

            val consultationAdded: Future[ConsultationAdded] =
              (repositoryActor ? AddConsultation(consultationDto.idPatient, Consultation(consultationDto.idTherapist, consultationDto.resume))).mapTo[ConsultationAdded]

            onComplete(consultationAdded) {
              case Success(_) => complete(StatusCodes.Created)
              case Failure(e) => complete(HttpResponse(StatusCodes.NotFound, entity = e.getMessage))
            }
          }
        } /*,
        // TODO: get according to id
        get {
          import spray.json.DefaultJsonProtocol._
          val users: Future[Seq[Therapist]] =
            (repositoryActor ? GetTherapists).mapTo[Seq[Therapist]]
          rejectEmptyResponse {
            complete(users)
          }
        }*/ )
    }
  }

}

