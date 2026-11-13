package com.oop.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import java.io.File

object ScoreManager {
    private const val RANK_FILE_PATH = "gamedata/rankings.txt"
    private var rankings = mutableListOf<Int>()

    init {
        loadRankings()
    }

    private fun loadRankings() {
        try {
            val file: FileHandle = Gdx.files.local(RANK_FILE_PATH)
            if (file.exists()) {
                val lines = file.readString().lineSequence()
                rankings = lines
                    .map { it.trim().toIntOrNull() }
                    .filterNotNull()
                    .sortedDescending()
                    .take(10)
                    .toMutableList()
            }
        } catch (e: Exception) {
            rankings = mutableListOf()
        }
    }

    fun getTop10(): List<Int> {
        return rankings
    }

    fun addScore(score: Int) {
        rankings.add(score)
        rankings.sortDescending()
        if (rankings.size > 10) {
            rankings = rankings.subList(0, 10)
        }
        saveRankings()
    }

    private fun saveRankings() {
        try {
            val file: FileHandle = Gdx.files.local(RANK_FILE_PATH)
            val content = rankings.joinToString("\n")
            file.writeString(content, false)
        } catch (e: Exception) {
            Gdx.app.error("ScoreManager", "Failed to save rankings: ${e.message}")
        }
    }
}
