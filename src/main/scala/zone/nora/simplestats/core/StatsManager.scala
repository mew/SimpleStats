package zone.nora.simplestats.core

import com.google.gson.JsonObject

/**
 * Manages game stats values without running into stupid null pointers.
 *
 * @param resp Player response JSON object.
 * @param game The name of the game.
 */
class StatsManager(resp: JsonObject, game: String) {

  val stats: JsonObject = try {
    resp.get("stats").getAsJsonObject.get(game).getAsJsonObject
  } catch {
    case _:Exception =>
      null
  }

  val achievements: JsonObject = try {
    resp.get("achievements").getAsJsonObject
  } catch {
    case _:Exception =>
      null
  }

  /**
   * Use this when you expect a Int response.
   *
   * @param name The name of the JSON key.
   * @param one Return one if response is zero. useful for handling arithmetic exceptions.
   * @return An actual integer or 0 if null.
   */
  def getStatsAsInt(name: String, one: Boolean = false): Int = try {
    val res = stats.get(name).getAsInt
    if (res == 0 && one) 1
    else res
  } catch {
    case _: Exception =>
      if (one) 1
      else 0
  }

  /**
   * Use this when you expect a Long response.
   *
   * @param name The name of the JSON key.
   * @return An actual Long or 0 if null.
   */
  def getStatsAsLong(name: String): Long = try {
    stats.get(name).getAsLong
  } catch {
    case _: Exception =>
      0
  }

  /**
   * Use this when you expect a Double response.
   *
   * @param name The name of the JSON key.
   * @return An actual Double or 0 if null.
   */
  def getStatsAsDouble(name: String): Double = try {
    stats.get(name).getAsDouble
  } catch {
    case _: Exception =>
      0.0
  }

  /**
   * Use this when you expect a String response.
   *
   * @param name The name of the JSON key
   * @return An actual String or "" if null
   */
  def getStatsAsString(name: String): String = try {
    stats.get(name).getAsString
  } catch {
    case _: Exception =>
      ""
  }
}
