package com.github.hedidata.domain

import org.bson.types.ObjectId

object Entities {

  final case class User(_id: Option[ObjectId], name: String, email: String, tags: List[Tag])
  final case class Tag(name: String)

  final case class Therapist(_id: Option[ObjectId], firstName: String, lastName: String, email: String)

  //TODO: add datetime
  final case class Consultation(_id: Option[ObjectId], idTherapist: ObjectId, resume: String)

  final case class Patient(
                            _id: Option[ObjectId],
                            firstName: String,
                            lastName: String,
                            address: String,
                            anamnesis: Option[String],
                            therapists: List[ObjectId],
                            consultations: List[Consultation]
                          )
}
