package dev.cheleb.ziojwt

import sttp.model.Uri

trait WithToken {
  def token: String
  val expiration: Long
}
