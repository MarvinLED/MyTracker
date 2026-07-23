package com.example.prokject2_tracker.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.ui.Modifier
import com.example.prokject2_tracker.analyse.AnalyseRoute
import com.example.prokject2_tracker.analyse.AnalyseScreen
import com.example.prokject2_tracker.fitness.FitnessRoute
import com.example.prokject2_tracker.fitness.FitnessScreen
import com.example.prokject2_tracker.fitness.cardio.CardioEditRoute
import com.example.prokject2_tracker.fitness.cardio.CardioEditScreen
import com.example.prokject2_tracker.nutrition.diary.DiaryAddEntryRoute
import com.example.prokject2_tracker.nutrition.diary.DiaryAddEntryScreen
import com.example.prokject2_tracker.nutrition.diary.DiaryRoute
import com.example.prokject2_tracker.nutrition.diary.DiaryScreen
import com.example.prokject2_tracker.nutrition.food.FoodEditRoute
import com.example.prokject2_tracker.nutrition.food.FoodEditScreen
import com.example.prokject2_tracker.nutrition.library.LibraryBackupRoute
import com.example.prokject2_tracker.nutrition.library.LibraryBackupScreen
import com.example.prokject2_tracker.nutrition.library.LibraryRoute
import com.example.prokject2_tracker.nutrition.library.LibraryScreen
import com.example.prokject2_tracker.nutrition.recipe.RecipeEditRoute
import com.example.prokject2_tracker.nutrition.recipe.RecipeEditScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = DiaryRoute,
        modifier = modifier,
    ) {
        composable<DiaryRoute> {
            DiaryScreen(
                onAddEntry = { epochDay -> navController.navigate(DiaryAddEntryRoute(epochDay)) },
            )
        }
        composable<DiaryAddEntryRoute> {
            DiaryAddEntryScreen(onDone = { navController.popBackStack() })
        }
        composable<LibraryRoute> {
            LibraryScreen(
                onAddFood = { navController.navigate(FoodEditRoute()) },
                onEditFood = { foodId -> navController.navigate(FoodEditRoute(foodId = foodId)) },
                onAddRecipe = { navController.navigate(RecipeEditRoute()) },
                onEditRecipe = { recipeId -> navController.navigate(RecipeEditRoute(recipeId = recipeId)) },
                onOpenBackup = { navController.navigate(LibraryBackupRoute) },
            )
        }
        composable<FoodEditRoute> {
            FoodEditScreen(onDone = { navController.popBackStack() })
        }
        composable<RecipeEditRoute> {
            RecipeEditScreen(onDone = { navController.popBackStack() })
        }
        composable<LibraryBackupRoute> {
            LibraryBackupScreen(onDone = { navController.popBackStack() })
        }
        composable<AnalyseRoute> {
            AnalyseScreen()
        }
        composable<FitnessRoute> {
            FitnessScreen(
                onAddCardioSession = { navController.navigate(CardioEditRoute()) },
                onEditCardioSession = { sessionId -> navController.navigate(CardioEditRoute(sessionId = sessionId)) },
            )
        }
        composable<CardioEditRoute> {
            CardioEditScreen(onDone = { navController.popBackStack() })
        }
    }
}
