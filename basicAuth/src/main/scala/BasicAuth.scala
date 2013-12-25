package info.schleichardt.play2.basicauth

import scala.beans.BeanProperty
import play.mvc.Http.{ RequestHeader => JRequestHeader }
import play.api.mvc.Action
import play.api.mvc.Handler
import play.api.mvc.Results._
import play.api.mvc

case class Credentials(@BeanProperty username: String, @BeanProperty password: String) {
  private[basicauth] def matches(otherUsername: String, otherPassword: String) = username == otherUsername && password == otherPassword
}

trait Authenticator {
  def authenticate(credentialsFromRequest: Credentials): Boolean
}

//you can retrieve the values from the environment as well via Typesafe config
class PlainCredentialsFromConfigAuthenticator extends Authenticator {

  import PlainCredentialsFromConfigAuthenticator._

  def authenticate(credentialsFromRequest: Credentials) = {
    val configuration = play.Play.application().configuration()
    val usernameOption = Option(configuration.getString(KeyUsername))
    val passwordOption = Option(configuration.getString(KeyPassword))
    (usernameOption, passwordOption) match {
      case (Some(username), Some(password)) => credentialsFromRequest.matches(username, password)
      case _ => {
        play.Logger.warn(s"$KeyUsername or $KeyPassword not set.")
        false
      }
    }
  }
}

object PlainCredentialsFromConfigAuthenticator {
  val KeyUsername = "basicAuth.username"
  val KeyPassword = "basicAuth.password"
}

class BasicAuth(authenticator: Authenticator) {
  def authenticate(request: mvc.RequestHeader, handler: => Handler): Option[Handler] = {
    val isAuthenticated = request.headers.get("Authorization").flatMap(extractAuthDataFromHeader).map(authenticator.authenticate(_)).getOrElse(false)
    if (isAuthenticated) {
      Option(handler)
    } else {
      Option(unauthorizedAction)
    }
  }

  def authenticate(request: JRequestHeader, handler: Handler): Handler = {
    val isAuthenticated = Option(request.getHeader("Authorization")).flatMap(extractAuthDataFromHeader).map(authenticator.authenticate(_)).getOrElse(false)
    if (isAuthenticated) {
      handler
    } else {
      unauthorizedAction
    }
  }

  def unauthorizedAction = Action {
    Unauthorized.withHeaders("WWW-Authenticate" -> """Basic realm="%s"""".format("Authentication needed"))
  }

  private def extractAuthDataFromHeader(header: String): Option[Credentials] = {
    //inspired from guillaumebort  https://gist.github.com/2328236 24.09.2012
    header.split(" ").drop(1).headOption.flatMap {
      encoded =>
        new String(org.apache.commons.codec.binary.Base64.decodeBase64(encoded.getBytes)).split(":").toList match {
          case userName :: password :: Nil if userName.length > 0 && password.length > 0 => Option(Credentials(userName, password))
          case _ => None
        }
    }
  }
}

