package info.schleichardt.play2.featuretoggle

import play.api.{ Play, Application, Plugin }
import play.mvc.{ Action => JAction, Results => JResults, SimpleResult, Http }
import play.libs.F.{ Promise => JPromise }

class FeatureTogglePlugin(app: Application) extends Plugin {
  override def enabled = app.configuration.getBoolean("info.schleichardt.play2.featuretoggle.enabled").getOrElse(true)

  def featuresAllEnabled(features: Class[_]*) = {
    val default = app.configuration.getBoolean("info.schleichardt.play2.featuretoggle.default").getOrElse(false)
    features.forall(feature => app.configuration.getBoolean(s"application.features.${feature.getSimpleName}").getOrElse(default))
  }
}

class FeatureInterceptor extends JAction[ImplementedFeature] {
  def call(ctx: Http.Context): JPromise[SimpleResult] = {
    val declaredFeaturesForAction: Array[Class[_]] = configuration.value
    val accessAllowed = Play.current.plugin(classOf[FeatureTogglePlugin]).map {
      plugin =>
        plugin.featuresAllEnabled(declaredFeaturesForAction: _*)
    }.getOrElse(false)
    if (accessAllowed) delegate.call(ctx)
    else JPromise.pure(JResults.notFound())
  }
}