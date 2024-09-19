package dev.cheleb.ziojwt

import sttp.model.Uri

trait WithToken {
  def issuer: Uri
  def token: String
  val expiration: Long
}
