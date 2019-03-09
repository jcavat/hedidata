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

trait PatientsRoutes extends JsonSupport {

  import domain.Entities._

  def repositoryActor: ActorRef

  private implicit lazy val timeout = Timeout(5.seconds)

  val patientDirective = pathPrefix("patients") {
    pathEnd {
      concat(
        post {
          entity(as[Patient]) { patient =>
            val newPatient = if (patient._id.isEmpty) patient.copy(_id = Some(new ObjectId())) else patient

            val userCreated: Future[ResourceCreated] =
              (repositoryActor ? Create(newPatient)).mapTo[ResourceCreated]

            onComplete(userCreated) {
              case Success(objectId) => complete((StatusCodes.Created, objectId))
              case Failure(e) => complete(HttpResponse(StatusCodes.InternalServerError, entity = e.toString))
            }
          }
        },
        get {
          import spray.json.DefaultJsonProtocol._
          val users: Future[Seq[Patient]] =
            (repositoryActor ? GetPatients).mapTo[Seq[Patient]]
          rejectEmptyResponse {
            complete(users)
          }
        })
    }
  }

}

