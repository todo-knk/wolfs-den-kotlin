package wolfsden.system

import wolfsden.entity.Entity
import wolfsden.entity.effects.Effect
import wolfsden.screen.WolfScreen
import wolfsden.system.GameStore.curEntities

object Scheduler : EntityListener {
    private var paused = false
    var clock = 1
    private var scheduled = curEntities.filter { it.hasTag("creature") }.sortedByDescending { it.stats!!.spd }

    init {
        GameStore.addListener(this)
    }

    fun updateScheduled() {
        scheduled = curEntities.filter { it.hasTag("creature") }.sortedByDescending { it.stats!!.spd }
    }

    override fun onAdd(entity: Entity) {
        updateScheduled()
    }

    override fun onRemove(entity: Entity) {
        updateScheduled()
    }

    fun tick() {
        if (!paused) {
            for (creature in scheduled) {
                creature.ai!!.delay--
                if (creature.ai!!.delay <= 0 && !creature.effectStack!!.loseTurn && creature.vit!!.alive) {
                    //log(clock, "Scheduler", "$creature acting on tick $clock")
                    when {
                        creature.isPlayer -> pause() //process player stuff
                        else -> {
                            creature.ai!!.getBTree().step()
                        }
                    }
                }
                val effsToRemove: MutableList<Effect> = mutableListOf()
                with(creature.effectStack!!) {
                    effects.forEach {
                        it.tick()
                        if (it.duration <= 0 && !it.permanent) effsToRemove.add(it)
                    }
                }
                effsToRemove.forEach {
                    creature.removeEffect(it)
                }
                creature.skillStack?.skills?.forEach {
                    it.tick()
                }
                if (!creature.vit!!.alive) {
                    if (creature.isPlayer) {
                        //WolfScreen.setScreen(GameOverScreen)
                    } else {
                        WolfScreen.addMessage("${creature.markupString} has been [Crimson]slain![]")
                        onDeath(creature)
                        GameStore.removeEntity(creature)
                    }
                }
            }
            //TODO: every tick actions
            clock++
        }
    }

    fun pause() {
        paused = true
    }

    fun resume() {
        paused = false
    }
}