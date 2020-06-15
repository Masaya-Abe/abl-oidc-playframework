package mvc

import model.User

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.json.{JsError, JsSuccess}
import cats.data.EitherT
import cats.implicits._
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

import scala.concurrent.duration._
import org.jose4j.jwa.AlgorithmConstraints.ConstraintType
import org.jose4j.jws.AlgorithmIdentifiers
import org.jose4j.jwt.consumer.JwtConsumerBuilder
import org.jose4j.jwx.JsonWebStructure
import play.api.Logger


// trait AuthenticateActionBuilder extends ActionBuilder[Request, AnyContent]
// object AuthenticateActionBuilder {
// }
//
// class AuthenticateActionBuilderImpl(
//   val parser: BodyParser[AnyContent]
// )(implicit val executionContext: ExecutionContext) extends AuthenticateActionBuilder {
//
//   def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]) = {
//     block(request)
//   }
// }

case class AuthenticateActionBuilder(
  val parser: BodyParser[AnyContent],
  val ws: WSClient
)(implicit val executionContext: ExecutionContext) extends ActionBuilder[Request, AnyContent] {

  private val logger                       = Logger(this.getClass())
  private val HEADER_AMZN_ACCESS_TOKEN     = "x-amzn-oidc-accesstoken"
  private val HEADER_AMZN_DATA_TOKEN       = "x-amzn-oidc-data"
  private val AMZN_PUBLIC_KEY_URL_TEMPLATE = "https://public-keys.auth.elb.ap-northeast-1.amazonaws.com/%s"
  private val DATA_TOKEN_ISSUER            = "https://login.microsoftonline.com/5f27d3dd-8c3b-4aff-a9cd-cd48b34e1add/v2.0"
  private val GRAPH_API_URI_USER_ME        = "https://graph.microsoft.com/v1.0/me"
  private val REQUEST_TIMEOUT              = 5
    .second

  def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]) = {
    val accessTokenOpt = request.headers.get(HEADER_AMZN_ACCESS_TOKEN)
    val dataTokenOpt   = request.headers.get(HEADER_AMZN_DATA_TOKEN)

    logger.info(accessTokenOpt.getOrElse("access token empty"))
    logger.info(dataTokenOpt.getOrElse("data token empty"))

    val authenticated = for {
      accessToken <- verifyAccessToken(accessTokenOpt)
      dataToken   <- verifyDataToken(dataTokenOpt)
      user        <- getUser(accessToken)
    } yield {
      // TODO : set resource to attrs
    }

    authenticated
      .semiflatMap(_ => block(request))
      .valueOr(v => v)
  }

  private def verifyAccessToken(accessTokenOpt: Option[String]) = {
    EitherT.fromEither[Future](
      accessTokenOpt match {
        case None    => {
          // TODO : logging
          Left(Unauthorized)
        }
        case Some(token) => Right(token)
      }
    )
  }

  private def verifyDataToken(dataTokenOpt: Option[String]) = {

    EitherT.fromOption[Future](
      dataTokenOpt,
      {
        // TODO : logging
        Unauthorized
      }
    ) flatMap(token => {
      val jwx = JsonWebStructure.fromCompactSerialization(token)
      val kid = jwx.getHeader("kid")

      //val jwt = JWT.decode(token)
      //val kid = jwt.getKeyId()

      for {
        publicKey <- getPublicKey(kid)
      } yield {
        val jwtConsumer = new JwtConsumerBuilder()
          .setRequireExpirationTime()           // the JWT must have an expiration time
          .setAllowedClockSkewInSeconds(30)     // allow some leeway in validating time based claims to account for clock skew
          .setRequireSubject()                  // the JWT must have a subject claim
          .setExpectedIssuer(DATA_TOKEN_ISSUER) // whom the JWT needs to have been issued by
          .setVerificationKey(publicKey)        // verify the signature with the public key
          .setJwsAlgorithmConstraints(          // only allow the expected signature algorithm(s) in the given context
            ConstraintType.WHITELIST,
            AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256
          )
          .build()

        // TODO : catch exception and to scala code
        jwtConsumer.processToClaims(token)
      }

    })
  }

  private def getPublicKey(kid: String) = {
    val publicKeyUrl = AMZN_PUBLIC_KEY_URL_TEMPLATE.format(kid)

    val requestResult = ws.url(publicKeyUrl)
      .withRequestTimeout(REQUEST_TIMEOUT)
      .get()
      .map { res => res.status match {
        case 200 => {
          val pubKey = res.body
            .replaceAll("\\r\\n", "")
            .replaceAll("\\n", "")
            .replaceAll("-----BEGIN PUBLIC KEY-----", "")
            .replaceAll("-----END PUBLIC KEY-----", "")

          val keyspec = new X509EncodedKeySpec(Base64.getDecoder().decode(pubKey))
          val kf      = KeyFactory.getInstance("EC")
          Right(kf.generatePublic(keyspec))
        }
        case _   => {
          // TODO : logging
          // "fail to get public key resource from %s (status code : %s)".format(
          //   publicKeyUrl,
          //   res.status
          // )

          Left(InternalServerError)
        }
      }}

    EitherT(requestResult)
  }

  private def getUser(accessToken: String) = {
    val requestResult = ws.url(GRAPH_API_URI_USER_ME)
      .withHttpHeaders("Authorization" -> "Bearer %s".format(accessToken))
      .withRequestTimeout(REQUEST_TIMEOUT)
      .get()
      .map { res => res.status match {
        case 200 => res.json.validate[User] match {
          case s: JsSuccess[User] => Right(s.get)
          case e: JsError         => {
            // TODO : logging response
            // "fail to get user resource from (response may be invalid format)"
            Left(InternalServerError)
          }
        }
        case _   => {
          // TODO : logging
          // "fail to get user resource from (status code : %s)".format(res.status)
          Left(InternalServerError)
        }
      }}

    EitherT(requestResult)
  }


}
