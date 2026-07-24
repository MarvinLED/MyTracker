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
import com.example.prokject2_tracker.fitness.TrainingEntryRoute
import com.example.prokject2_tracker.fitness.TrainingEntryScreen
import com.example.prokject2_tracker.fitness.cardio.CardioActivityTypeManageRoute
import com.example.prokject2_tracker.fitness.cardio.CardioActivityTypeManageScreen
import com.example.prokject2_tracker.fitness.strength.MuscleGroupManageRoute
import com.example.prokject2_tracker.fitness.strength.MuscleGroupManageScreen
import com.example.prokject2_tracker.fitness.strength.StrengthExerciseEditRoute
import com.example.prokject2_tracker.fitness.strength.StrengthExerciseEditScreen
import com.example.prokject2_tracker.fitness.strength.StrengthExerciseLibraryRoute
import com.example.prokject2_tracker.fitness.strength.StrengthExerciseLibraryScreen
import com.example.prokject2_tracker.fluid.FluidRoute
import com.example.prokject2_tracker.fluid.FluidScreen
import com.example.prokject2_tracker.fluid.FluidTypeManageRoute
import com.example.prokject2_tracker.fluid.FluidTypeManageScreen
import com.example.prokject2_tracker.fluid.FluidUnitManageRoute
import com.example.prokject2_tracker.fluid.FluidUnitManageScreen
import com.example.prokject2_tracker.goals.GoalsRoute
import com.example.prokject2_tracker.goals.GoalsScreen
import com.example.prokject2_tracker.habit.HabitRoute
import com.example.prokject2_tracker.habit.HabitScreen
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
import com.example.prokject2_tracker.overview.OverviewRoute
import com.example.prokject2_tracker.overview.OverviewScreen
import com.example.prokject2_tracker.weight.WeightRoute
import com.example.prokject2_tracker.weight.WeightScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = OverviewRoute,
        modifier = modifier,
    ) {
        composable<OverviewRoute> {
            OverviewScreen(onOpenDrawer = onOpenDrawer)
        }
        composable<DiaryRoute> {
            DiaryScreen(
                onAddEntry = { epochDay -> navController.navigate(DiaryAddEntryRoute(epochDay)) },
                onOpenDrawer = onOpenDrawer,
            )
        }
        composable<FluidRoute> {
            FluidScreen(
                onOpenDrawer = onOpenDrawer,
                onOpenTypeManagement = { navController.navigate(FluidTypeManageRoute) },
                onOpenUnitManagement = { navController.navigate(FluidUnitManageRoute) },
            )
        }
        composable<FluidTypeManageRoute> {
            FluidTypeManageScreen(onBack = { navController.popBackStack() })
        }
        composable<FluidUnitManageRoute> {
            FluidUnitManageScreen(onBack = { navController.popBackStack() })
        }
        composable<HabitRoute> {
            HabitScreen(onOpenDrawer = onOpenDrawer)
        }
        composable<WeightRoute> {
            WeightScreen(onOpenDrawer = onOpenDrawer)
        }
        composable<DiaryAddEntryRoute> {
            DiaryAddEntryScreen(
                onDone = { navController.popBackStack() },
                onCreateFood = { navController.navigate(FoodEditRoute()) },
                onCreateRecipe = { navController.navigate(RecipeEditRoute()) },
            )
        }
        composable<LibraryRoute> {
            LibraryScreen(
                onAddFood = { navController.navigate(FoodEditRoute()) },
                onEditFood = { foodId -> navController.navigate(FoodEditRoute(foodId = foodId)) },
                onAddRecipe = { navController.navigate(RecipeEditRoute()) },
                onEditRecipe = { recipeId -> navController.navigate(RecipeEditRoute(recipeId = recipeId)) },
                onOpenBackup = { navController.navigate(LibraryBackupRoute) },
                onOpenDrawer = onOpenDrawer,
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
            AnalyseScreen(onOpenDrawer = onOpenDrawer)
        }
        composable<GoalsRoute> {
            GoalsScreen(onOpenDrawer = onOpenDrawer)
        }
        composable<FitnessRoute> {
            FitnessScreen(
                onAddTraining = { navController.navigate(TrainingEntryRoute()) },
                onEditCardioSession = { sessionId -> navController.navigate(TrainingEntryRoute(cardioSessionId = sessionId)) },
                onEditStrengthLogEntry = { entryId -> navController.navigate(TrainingEntryRoute(strengthLogEntryId = entryId)) },
                onOpenExerciseLibrary = { navController.navigate(StrengthExerciseLibraryRoute) },
                onOpenMuscleGroupLibrary = { navController.navigate(MuscleGroupManageRoute) },
                onOpenCardioActivityTypeLibrary = { navController.navigate(CardioActivityTypeManageRoute) },
                onOpenDrawer = onOpenDrawer,
            )
        }
        composable<TrainingEntryRoute> {
            TrainingEntryScreen(onDone = { navController.popBackStack() })
        }
        composable<CardioActivityTypeManageRoute> {
            CardioActivityTypeManageScreen(onBack = { navController.popBackStack() })
        }
        composable<MuscleGroupManageRoute> {
            MuscleGroupManageScreen(onBack = { navController.popBackStack() })
        }
        composable<StrengthExerciseLibraryRoute> {
            StrengthExerciseLibraryScreen(
                onBack = { navController.popBackStack() },
                onAddExercise = { navController.navigate(StrengthExerciseEditRoute()) },
                onEditExercise = { exerciseId -> navController.navigate(StrengthExerciseEditRoute(exerciseId = exerciseId)) },
            )
        }
        composable<StrengthExerciseEditRoute> {
            StrengthExerciseEditScreen(onDone = { navController.popBackStack() })
        }
    }
}
