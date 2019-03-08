package com.github.hedidata

import com.github.hedidata.repository.MongoRepositoryActor.UserCreated
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.bson.types.ObjectId
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

  implicit val tagJsonFormat = jsonFormat1(Tag)
  implicit val userJsonFormat = jsonFormat4(User)
  implicit val userCreatedJsonFormat = jsonFormat1(UserCreated)
}
