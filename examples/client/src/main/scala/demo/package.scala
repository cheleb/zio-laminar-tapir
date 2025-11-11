package demo

import dev.cheleb.ziotapir.*
import dev.cheleb.ziojwt.WithToken

import zio.prelude.*
import zio.json.*
case class UserToken(token: String, expiration: Long)
    extends dev.cheleb.ziojwt.WithToken derives JsonDecoder

trait JsonEncoderContravariant[-A] { self =>
  def encode(a: A): String
  def contramap[B](f: B => A): JsonEncoderContravariant[B] =
    new JsonEncoderContravariant[B] {
      def encode(b: B): String =
        self.encode(f(b))
    }
}
object JsonEncoder {
  implicit val JsonEncoderContravariant
      : Contravariant[JsonEncoderContravariant] =
    new Contravariant[JsonEncoderContravariant] {
      def contramap[A, B](
          f: B => A
      ): JsonEncoderContravariant[A] => JsonEncoderContravariant[B] =
        jsonEncoder => jsonEncoder.contramap(f)
    }
}

object UserToken {
//  implicit val jsonEncoder: JsonEncoder[UserToken] = JsonEncoder.d
}
given JsonCodec[WithToken] = DeriveJsonCodec
  .gen[UserToken]
  .transform(
    ut => UserToken(ut.token, ut.expiration),
    wt => UserToken(wt.token, wt.expiration)
  )

given session: Session[UserToken] = SessionLive[UserToken]
