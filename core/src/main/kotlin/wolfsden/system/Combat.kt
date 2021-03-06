package wolfsden.system

import wolfsden.CommonColors
import wolfsden.entity.Entity
import wolfsden.joinWithAnd
import wolfsden.screen.WolfScreen

private fun Entity.countPrefix(pattern: String): Int {
    val p = pattern.toRegex()
    return tags.map {
        p.matchEntire(it)?.destructured?.let { (pref) ->
            pref.length
        } ?: 0
    }.sum()
}

fun Entity.hasTag(tag: String) = tags.contains(tag)
fun Entity.hasWeakness(weak: String): Int = countPrefix("""^(X+)-$weak""")
fun Entity.hasResistance(resist: String): Int = countPrefix("""^(O+)-$resist""")

data class CombatResults(
    val attacker: Entity,
    val defender: Entity,
    val hit: Boolean = false,
    val hitBy: Int = 0,
    val dmg: Int = 0,
    val weakness: List<String> = listOf(),
    val resistance: List<String> = listOf()
)

fun Entity.attack(other: Entity, atkStat: Int = this.atk, defStat: Int = other.dfp, dmgMod: Int = 0): CombatResults {
    val toDef = if (other.hasEffect("Stunned")) 0 else defStat
    val (sux, hitBy) = WolfRNG.roll(atkStat, toDef)
    val hit = sux > 0
    var (dmg, _) = if (hit) WolfRNG.roll((sux - 1) * 2 + this.dmg + dmgMod) else 0 to 0
    val wk: MutableList<String> = mutableListOf()
    val res: MutableList<String> = mutableListOf()
    this.atkTags.forEach {
        val weakLevel = other.hasWeakness(it)
        val resLevel = other.hasResistance(it)
        if (weakLevel > 0) {
            wk.add(it)
            when (weakLevel) {
                1 -> dmg = (dmg * 1.5).toInt()
                2 -> dmg *= 2
                3 -> dmg *= 3
                else -> dmg *= 4
            }
        }
        if (resLevel > 0) {
            res.add(it)
            when (resLevel) {
                1 -> dmg = (dmg * .75).toInt()
                2 -> dmg /= 2
                3 -> dmg /= 4
                else -> dmg = 0
            }
        }
    }
    if (hit) other.aggroStack!!.changeAggro(this.eID, dmg)
    return CombatResults(this, other, hit, hitBy, dmg, wk, res)
}

fun autoAttack(atkr: Entity, defr: Entity) {
    val results = atkr.attack(defr)
    if (results.hit) defr.takeDmg(results.dmg)
    describeCombat(results)
}

fun describeCombat(result: CombatResults) {
    with(result) {
        val warning = CommonColors.WARNING
        val vit = CommonColors.VIT
        val info = CommonColors.INFO
        val verb: String = if (hit) {
            val pctDmg = maxOf(dmg * 100 / defender.maxVit, 0)
            when (pctDmg) {
                0 -> "[$warning]fails to harm[]"
                in (1 until 10) -> "[$warning]nicks[]"
                in (10 until 30) -> "[$warning]strikes[]"
                in (30 until 50) -> "[$warning]soundly strikes[]"
                else -> "[$vit]wrecks[]"
            }
        } else {
            when (hitBy) {
                in (1 until defender.stats!!.spd) -> "[$warning]barely misses[]"
                in (defender.stats!!.spd until defender.dfp) -> "[$warning]clashes with[]"
                else -> "[$warning]misses[]"
            }
        }
        val wk = if (weakness.isNotEmpty()) {
            ", exposing ${defender.markupString}'s [$info]${weakness.joinWithAnd()}[] weakness"
        } else {
            ""
        }
        val res = if (resistance.isNotEmpty()) {
            ", meeting ${defender.markupString}'s [$info]${resistance.joinWithAnd()}[] resistance"
        } else {
            ""
        }
        val dm = if (hit && dmg > 0) ", dealing [$vit]$dmg damage[]" else ""
        val finalString = "${attacker.markupString} $verb ${defender.markupString}$wk$res$dm!"
        WolfScreen.addMessage(finalString)
    }
}