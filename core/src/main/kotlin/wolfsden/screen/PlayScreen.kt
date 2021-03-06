package wolfsden.screen

import com.badlogic.gdx.ai.fsm.DefaultStateMachine
import com.badlogic.gdx.graphics.Colors
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.StretchViewport
import squidpony.squidgrid.Direction
import squidpony.squidgrid.gui.gdx.SColor
import squidpony.squidgrid.gui.gdx.SquidInput
import squidpony.squidmath.Coord
import wolfsden.CommonColors
import wolfsden.between
import wolfsden.entity.Entity
import wolfsden.getColor
import wolfsden.screen.ui.MenuState
import wolfsden.system.GameStore
import wolfsden.system.GameStore.curMap
import wolfsden.system.Scheduler
import wolfsden.system.playerVisible
import wolfsden.toICString

class PlayScreen(batch: SpriteBatch) : WolfScreen("main") {
    val mapW = 80
    val mapH = 30
    private val statW = 40
    private val statH = 10
    private val msgW = 40
    val msgH = 10
    private val ttW = 40
    private val ttH = 10
    private val sklW = 40
    private val sklH = 12
    private val invW = 40
    private val invH = 12
    private val eqW = 40
    private val eqH = 6

    override val vport = StretchViewport(fullPixelW, fullPixelH)

    private val playLayout = layout(vport, batch) {
        layers {
            id = "map"
            gw = mapW
            gh = mapH
            x = 0
            y = msgH
            tcf {
                tweakWidth = 1.1f
                tweakHeight = 1.1f
            }
        }
        messages {
            id = "messages"
            gw = msgW
            gh = msgH
            x = statW
            y = 0
            tcf {
                tweakWidth = 1.1f
                tweakHeight = 1.25f
            }
        }
        panel {
            id = "stats"
            gw = statW
            gh = statH
            x = 0
            y = 0
            tcf {
                tweakWidth = 1.1f
                tweakHeight = 1.25f
            }
        }
        panel {
            id = "tt"
            gw = ttW
            gh = ttH
            x = statW + msgW
            y = 0
            tcf {
                tweakWidth = 1.1f
                tweakHeight = 1.25f
            }
        }
        panel {
            id = "skills"
            gw = sklW
            gh = sklH
            x = mapW
            y = ttH
            tcf {
                tweakWidth = 1.1f
                tweakHeight = 1.25f
            }
        }
        panel {
            id = "inventory"
            gw = invW
            gh = invH
            x = mapW
            y = ttH + sklH
            tcf {
                tweakWidth = 1.1f
                tweakHeight = 1.25f
            }
        }
        panel {
            id = "equip"
            gw = eqW
            gh = eqH
            x = mapW
            y = ttH + sklH + invH
            tcf {
                tweakWidth = 1.1f
                tweakHeight = 1.25f
            }
        }
    }
    override var input: SquidInput = SquidInput()
    val mapLayers = playLayout.toSparseLayers("map")
    private val statPanel = playLayout.toSquidPanel("stats")
    private val msgPanel = playLayout.toMessageBox("messages")
    private val ttPanel = playLayout.toSquidPanel("tt")
    private val sklPanel = playLayout.toSquidPanel("skills")
    private val invPanel = playLayout.toSquidPanel("inventory")
    private val eqPanel = playLayout.toSquidPanel("equip")
    var curState = DefaultStateMachine<PlayScreen, MenuState>(this, MenuState.NULL)
    val menuVPort = StretchViewport(fullPixelW, fullPixelH)
    val menuStage = Stage(menuVPort, batch)

    override val stage = playLayout.build()

    private val FW = SColor.FLOAT_WHITE
    private val player
        get() = GameStore.player


    private val cam: Coord
        get() {
            val m = GameStore.curMap
            val c = GameStore.player.pos!!.coord
            val calc: (Int, Int, Int) -> Int = { p, md, s -> MathUtils.clamp(p - s / 2, 0, maxOf(md - s, 0)) }
            val camX = calc(c.x, m.width, mapW)
            val camY = calc(c.y, m.height, mapH)
            return Coord.get(camX, camY)
        }

    var cursor: Coord? = null

    private fun drawDungeon() {
        mapLayers.clear()
        val m = GameStore.curMap
        val GF = SColor.GRAY.toFloatBits()
        for (x in 0.until(mapW)) {
            for (y in 0.until(mapH)) {
                val wx = x + cam.x
                val wy = y + cam.y
                val wc = Coord.get(wx, wy)
                if (!m.oob(wc)) {
                    val c = m.displayMap[wx][wy]
                    val fg = m.fgFloats[wx][wy]
                    val bg = m.bgFloats[wx][wy]
                    when {
                        player.visible(wc) -> {
                            mapLayers.put(x, y, c, fg, bg)
                        }
                        m.light -> {
                            mapLayers.put(x, y, c, fg, SColor.lerpFloatColors(bg, GF, -0.5f))
                        }
                        else -> {
                        }
                    }
                }
            }
        }
    }

    private fun drawStats() {
        val markupStat = { label: String, stat: Number, col: Int, y: Int ->
            var placement = Pair(0, 0)
            when (col) {
                1 -> placement = Pair(1, 5)
                2 -> placement = Pair(8, 12)
                3 -> placement = Pair(30, 34)
            }
            statPanel.put(placement.first, y, markup(label, CommonColors.INFO))
            statPanel.put(placement.second, y, stat.toString())
        }
        val markupTemper = { label: String, tMin: Number, tMax: Number, startx: Int, y: Int, color: String ->
            val numFormat = when (tMin) {
                is Int -> "%4d/%4d"
                else -> "%5.0f/%5.0f"
            }
            statPanel.put(startx, y, "[$color]$label[] $numFormat".format(tMin, tMax).toICString())
        }
        with(statPanel) {
            erase()
            putBorders(FW, "Stats")
            put(1, 1, player.markupString!!.toICString())
            put(1, 2, "${curMap.name} ${player.pos!!.coord}")
            markupStat("Str", player.stats!!.str, 1, 3)
            markupStat("Sta", player.stats!!.stam, 1, 4)
            markupStat("Spd", player.stats!!.spd, 1, 5)
            markupStat("Skl", player.stats!!.skl, 1, 6)
            markupStat("Dmg", player.dmg, 2, 3)
            markupStat("Sav", player.sav, 2, 4)
            markupStat("Dfp", player.dfp, 2, 5)
            markupStat("Atk", player.atk, 2, 6)
            markupTemper("Vit", player.vit!!.curVit, player.maxVit, 15, 3, CommonColors.VIT)
            markupTemper("End", player.vit!!.curEnd, player.maxEnd, 15, 4, CommonColors.WARNING)
            markupTemper("Arm", player.curArmor, player.maxArmor, 15, 5, CommonColors.METAL)
            markupTemper("Shd", player.curShield, player.maxShield, 15, 6, CommonColors.SHIELD)
            markupStat("MDl", player.movDly, 3, 3)
            markupStat("ADl", player.atkDly, 3, 4)
            markupTemper("XP", player.xp!!.curXP, player.xp!!.totXP, 1, 7, CommonColors.XP)
        }
    }

    private fun drawMsgs() {
        msgPanel.erase()
        msgPanel.putBorders(FW, "Messages")
    }

    private fun drawTT() {
        ttPanel.erase()
        ttPanel.putBorders(FW, "Info")
        var idx = 1
        player.effectStack!!.effects.forEach {
            when (idx) {
                in (1 until 8) -> ttPanel.put(1, idx, it.toString().toICString())
                8 -> ttPanel.put(1, 8, "...")
                else -> {
                }
            }
            idx++
        }
    }

    private fun drawSkl() {
        sklPanel.erase()
        sklPanel.putBorders(FW, "Skills(Shift: use)")
        if (player.skillStack!!.skills.isNotEmpty()) {
            player.skillStack!!.skillTable.forEachIndexed { index, pair ->
                sklPanel.put(1, index + 1,
                             "[${CommonColors.INFO}]${pair.first}:[] ${pair.second.markupString}".toICString())
            }
        }
    }

    private fun drawInv() {
        invPanel.erase()
        invPanel.putBorders(FW, "Inventory(number: use, Alt: describe)")
        for ((idx, item) in player.inventory.withIndex()) {
            invPanel.put(1, 1 + idx, "$idx: ${item.markupString}".toICString())
        }
    }

    private fun drawEQ() {
        with(eqPanel) {
            erase()
            putBorders(FW, "Equipment")
            put(1, 1, player.mhMarkup)
            put(1, 2, player.ohMarkup)
            put(1, 3, player.armorMarkup)
            put(1, 4, player.trinketMarkup)
        }
    }

    private fun drawHUD() {
        drawStats()
        drawMsgs()
        drawTT()
        drawSkl()
        drawInv()
        drawEQ()
    }

    private fun drawEntities() {
        var ec: Coord
        val toDraw = GameStore.curEntities.filter { it.pos != null && it.playerVisible() }
        for (entity in toDraw.sortedBy { it.draw!!.layer }) {
            ec = entity.pos!!.coord
            val color = entity.draw!!.color
            mapLayers.put(ec.x - cam.x, ec.y - cam.y, entity.draw!!.glyph, Colors.get(color).toFloatBits())
        }
    }

    fun addMessage(msg: String) {
        msgPanel.appendWrappingMessage("-$msg".toICString())
    }

    fun addMessageVisible(other: Entity, msg: String) {
        if (GameStore.player.visible(other)) addMessage(msg)
    }

    private fun inView(c: Coord): Boolean {
        val onMap = !curMap.oob(c)
        val onScreen = (c.x - cam.x).between(0, mapW - 1) && (c.y - cam.y).between(0, mapH - 1)
        return onMap && onScreen
    }

    fun moveCursor(direction: Direction) {
        val newC = cursor?.translate(direction) ?: return
        if (inView(newC)) cursor = newC
        GameStore.update()
    }

    private fun drawCursor() {
        val skl = player.ai!!.skillInUse
        //skl?.aoe?.shift(cursor)
        val color = if (skl?.canTarget(cursor!!) == true) {
            CommonColors.WARNING.getColor()
        } else {
            CommonColors.VIT.getColor()
        }
        mapLayers.put(cursor!!.x - cam.x, cursor!!.y - cam.y, 'X', color)
    }

    override fun enter() {
        curState.changeState(MenuState.PLAY)
        super.enter()
    }

    override fun render() {
        if (GameStore.mapDirty) {
            drawDungeon()
            drawEntities()
            if (cursor != null) drawCursor()
            GameStore.mapDirty = false
        }
        if (GameStore.hudDirty) {
            drawHUD()
            GameStore.hudDirty = false
        }
        Scheduler.tick()
        if (input.hasNext()) input.next()

        stage.act()
        stage.draw()

        menuStage.act()
        menuStage.viewport.apply(false)
        menuStage.draw()
    }

    var itemSelected: Entity? = null
}