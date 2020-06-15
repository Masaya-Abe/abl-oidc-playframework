package mvc

import play.api.libs.ws.WSClient
import play.api.mvc.{ActionBuilder, AnyContent, BaseControllerHelpers, Request}

trait AuthExtensionMethods extends {
  self: BaseControllerHelpers =>

  val ws: WSClient

  /** The ExecutionContext with using on Playframework. */
  implicit lazy val executionContext = defaultExecutionContext

  def Authenticate(): ActionBuilder[Request, AnyContent] =
    AuthenticateActionBuilder(parse.default, ws)

}
