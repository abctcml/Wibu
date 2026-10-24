package com.animapper.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.animapper.app.ui.detail.DetailScreen
import com.animapper.app.ui.home.HomeScreen
import com.animapper.app.ui.player.PlayerScreen
import com.animapper.app.ui.search.SearchScreen

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onOpenDetail = { id -> nav.navigate("detail/$id") },
                onOpenSearch = { nav.navigate("search") },
            )
        }
        composable("search") {
            SearchScreen(
                onBack = { nav.popBackStack() },
                onOpenDetail = { id -> nav.navigate("detail/$id") },
            )
        }
        composable("detail/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { e ->
            val id = e.arguments?.getInt("id") ?: return@composable
            DetailScreen(
                id = id,
                onBack = { nav.popBackStack() },
                onPlay = { epId, prov, srv ->
                    nav.navigate("player/$epId/$prov/${srv ?: "_"}")
                },
            )
        }
        composable("player/{episodeId}/{provider}/{server}") { e ->
            val epId = e.arguments?.getString("episodeId") ?: return@composable
            val prov = e.arguments?.getString("provider") ?: return@composable
            val srv = e.arguments?.getString("server")?.takeIf { it != "_" }
            PlayerScreen(
                episodeId = epId, provider = prov, server = srv,
                onBack = { nav.popBackStack() },
            )
        }
    }
}
