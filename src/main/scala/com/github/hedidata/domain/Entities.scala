package com.github.hedidata.domain

import org.bson.types.ObjectId

object Entities {

  sealed trait Entity

  final case class Therapist(_id: Option[ObjectId], firstName: String, lastName: String, email: String) extends Entity

  //TODO: add datetime
  final case class Consultation(idTherapist: ObjectId, resume: String)

  final case class EntityObjectId(id: ObjectId)

  final case class Patient(
    _id: Option[ObjectId],
    firstName: String,
    lastName: String,
    address: String,
    anamnesis: Option[String],
    therapists: List[ObjectId],
    consultations: List[Consultation]) extends Entity
}
