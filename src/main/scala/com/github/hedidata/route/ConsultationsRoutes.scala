package com.github.hedidata.route

import akka.actor.ActorRef
import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives.{ entity, _ }
import akka.http.scaladsl.server.{ ExceptionHandler, Route }
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

  def exceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case _: IllegalArgumentException =>
        extractUri { id =>
          println(s"$id is malformated")
          complete(HttpResponse(StatusCodes.BadRequest, entity = s"$id is malformated and does not conform to an ObjectId"))
        }
    }

  val consultationsDirective = handleExceptions(exceptionHandler) {
    pathPrefix("consultations") {
      pathEnd {
        post {
          entity(as[ConsultationDto]) { consultationDto =>

            val consultationAdded: Future[ConsultationAdded] =
              (repositoryActor ? AddConsultation(consultationDto.idPatient, Consultation(consultationDto.idTherapist, consultationDto.resume))).mapTo[ConsultationAdded]

            onComplete(consultationAdded) {
              case Success(_) => complete(StatusCodes.Created)
              case Failure(e) => {
                println("-" + e)
                complete(HttpResponse(StatusCodes.NotFound, entity = e.getMessage))
              }
            }
          }
        }
      } ~
        pathPrefix(Segment) { segment =>
          get {
            import spray.json.DefaultJsonProtocol._
            val consultations: Future[Seq[Consultation]] =
              (repositoryActor ? GetConsultations(new ObjectId(segment))).mapTo[Seq[Consultation]]
            rejectEmptyResponse {
              complete(consultations)
            }

          }
        }
    }
  }

}

