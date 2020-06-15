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

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index = Authenticate().async {
    Future.successful(
      Ok(views.html.index("Your new application is ready."))
    )
  }

  def logout = Authenticate().async { implicit request =>
    Future.successful(
      Ok(views.html.index("Your new application is ready."))
        .discardingCookies(DiscardingCookie("AWSELBAuthSessionCookie-1"))
        .discardingCookies(DiscardingCookie("AWSELBAuthSessionCookie-0"))
    )
  }
}