package com.github.hedidata.domain

import org.bson.types.ObjectId

object Entities {

  final case class User(_id: Option[ObjectId], name: String, email: String, tags: List[Tag])
  final case class Tag(name: String)

}
