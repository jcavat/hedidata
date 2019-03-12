package com.github.hedidata

import com.github.hedidata.repository.MongoRepositoryActor.ResourceCreated
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.github.hedidata.route.ConsultationDto
import com.github.hedidata.route.LoginRoutesWithAuth.LoginRequest
import org.mongodb.scala.bson.ObjectId
import spray.json.{ DefaultJsonProtocol, DeserializationException, JsString, JsValue, JsonFormat }

trait JsonSupport extends SprayJsonSupport {

  import domain.Entities._

  import DefaultJsonProtocol._

  implicit object ObjectIdJsonFormat extends JsonFormat[ObjectId] {
    def write(obj: ObjectId): JsValue = JsString(obj.toString)

    def read(json: JsValue): ObjectId = json match {
      case JsString(str) => new ObjectId(str)
      case _ => throw new DeserializationException(" string expected")
    }
  }

  implicit val userCreatedJsonFormat = jsonFormat1(ResourceCreated)
  implicit val therapistJsonFormat = jsonFormat4(Therapist)
  implicit val therapistIdJsonFormat = jsonFormat1(EntityObjectId)
  implicit val consultationDtoJsonFormat = jsonFormat3(ConsultationDto)
  implicit val consultationJsonFormat = jsonFormat2(Consultation)
  implicit val patientJsonFormat = jsonFormat7(Patient)
  implicit val loginRequestJsonFormat = jsonFormat2(LoginRequest)

}
