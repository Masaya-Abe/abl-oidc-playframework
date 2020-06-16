package controllers

import javax.inject._
import mvc.AuthExtensionMethods
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.concurrent.Future

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents, iws: WSClient) extends
AbstractController(cc) with AuthExtensionMethods {

  val ws: WSClient = iws

  def ping = Action async {
    Future.successful(
      Ok("ping ok")
    )
  }

  def index = Authenticate().async {
    Future.successful(
      Ok(views.html.index("Your new application is ready."))
    )
  }

  def logout = Authenticate().async { implicit request =>
    val cookies = Seq("AWSELBAuthSessionCookie-0", "AWSELBAuthSessionCookie-1")
    Future.successful(
      Ok(views.html.logout(cookies))
    )
  }
}
