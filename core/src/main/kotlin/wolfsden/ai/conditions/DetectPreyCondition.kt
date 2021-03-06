package wolfsden.ai.conditions

import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task
import wolfsden.entity.Entity
import wolfsden.log
import wolfsden.system.Scheduler.clock
import wolfsden.system.visibleEnemies

class DetectPreyCondition : LeafTask<Entity>() {
    override fun execute(): Status {
        val prey = `object`.visibleEnemies()
        return if (prey.isEmpty()) {
            log(clock, "AI", "${`object`} fails to find prey")
            Status.FAILED
        } else {
            log(clock, "AI", "${`object`} finds prey")
            Status.SUCCEEDED
        }
    }

    override fun copyTo(task: Task<Entity>?): Task<Entity> {
        return task!!
    }
}