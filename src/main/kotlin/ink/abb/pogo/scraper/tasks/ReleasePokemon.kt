/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.pokemon.getIv
import ink.abb.pogo.scraper.util.pokemon.getIvPercentage

class ReleasePokemon : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        val groupedPokemon = ctx.api.inventories.pokebank.pokemons.groupBy { it.pokemonId }
        val ignoredPokemon = settings.ignoredPokemon
        val obligatoryTransfer = settings.obligatoryTransfer
        val minIVPercentage = settings.transferIVThreshold
        val minCP = settings.transferCPThreshold

        groupedPokemon.forEach {
            val sorted = it.value.sortedByDescending { it.cp }
            for ((index, pokemon) in sorted.withIndex()) {
                // don't drop favourited or nicknamed pokemon
                val isFavourite = pokemon.nickname.isNotBlank() || pokemon.favorite
                if (!isFavourite) {
                    val iv = pokemon.getIv()
                    val ivPercentage = pokemon.getIvPercentage()
                    // never transfer highest rated Pokemon
                    if (index > 0) {
                        // stop releasing when pokemon is set in ignoredPokemon
                        if (!ignoredPokemon.contains(pokemon.pokemonId.name)) {
                            var shouldRelease = obligatoryTransfer.contains(pokemon.pokemonId.name)
                            var reason = ""
                            if (shouldRelease) {
                                reason = "Obligatory release"
                            } else {
                                // never transfer > min IV percentage (unless set to -1)
                                if (ivPercentage < minIVPercentage || minIVPercentage == -1) {
                                    reason = "IV < minimum IV percentage"
                                    shouldRelease = true
                                }
                                // never transfer > min CP  (unless set to -1)
                                if (pokemon.cp < minCP || minCP == -1) {
                                    reason = "CP < minimum CP"
                                    // only set it to true, when it already was true, otherwise don't release
                                    shouldRelease = shouldRelease && true
                                }
                            }
                            if (shouldRelease) {
                                ctx.pokemonStats.second.andIncrement
                                Log.yellow("Going to transfer ${pokemon.pokemonId.name} with " +
                                        "CP ${pokemon.cp} and IV $iv%; reason: $reason")
                                pokemon.transferPokemon()
                            }
                        }
                    }
                }
            }
        }
    }
}
