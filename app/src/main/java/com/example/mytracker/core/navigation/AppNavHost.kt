package com.example.mytracker.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import androidx.compose.ui.Modifier
import com.example.mytracker.achievements.AchievementsRoute
import com.example.mytracker.achievements.AchievementsScreen
import com.example.mytracker.analyse.AnalyseRoute
import com.example.mytracker.analyse.AnalyseScreen
import com.example.mytracker.bloodpressure.BloodPressureRoute
import com.example.mytracker.bloodpressure.BloodPressureScreen
import com.example.mytracker.core.backup.BackupRoute
import com.example.mytracker.core.backup.BackupScreen
import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.fitness.FitnessRoute
import com.example.mytracker.fitness.FitnessScreen
import com.example.mytracker.fitness.TrainingHistoryRoute
import com.example.mytracker.fitness.TrainingHistoryScreen
import com.example.mytracker.fitness.cardio.CardioActivityDetailRoute
import com.example.mytracker.fitness.cardio.CardioActivityDetailScreen
import com.example.mytracker.fitness.cardio.CardioActivityTypeManageRoute
import com.example.mytracker.fitness.cardio.CardioActivityTypeManageScreen
import com.example.mytracker.fitness.strength.MuscleGroupManageRoute
import com.example.mytracker.fitness.strength.MuscleGroupManageScreen
import com.example.mytracker.fitness.strength.StrengthExerciseDetailRoute
import com.example.mytracker.fitness.strength.StrengthExerciseDetailScreen
import com.example.mytracker.fitness.strength.StrengthExerciseEditRoute
import com.example.mytracker.fitness.strength.StrengthExerciseEditScreen
import com.example.mytracker.fitness.strength.StrengthExerciseLibraryRoute
import com.example.mytracker.fitness.strength.StrengthExerciseLibraryScreen
import com.example.mytracker.fluid.FluidQuickAddManageRoute
import com.example.mytracker.fluid.FluidQuickAddManageScreen
import com.example.mytracker.fluid.FluidRoute
import com.example.mytracker.fluid.FluidScreen
import com.example.mytracker.fluid.FluidTypeManageRoute
import com.example.mytracker.fluid.FluidTypeManageScreen
import com.example.mytracker.fluid.FluidUnitManageRoute
import com.example.mytracker.fluid.FluidUnitManageScreen
import com.example.mytracker.goals.DayGoalsRoute
import com.example.mytracker.goals.DayGoalsScreen
import com.example.mytracker.goals.GoalsRoute
import com.example.mytracker.goals.GoalsScreen
import com.example.mytracker.habit.HabitRoute
import com.example.mytracker.habit.HabitScreen
import com.example.mytracker.measurement.BodySiteManageRoute
import com.example.mytracker.measurement.BodySiteManageScreen
import com.example.mytracker.measurement.MeasurementRoute
import com.example.mytracker.measurement.MeasurementScreen
import com.example.mytracker.nutrition.diary.DiaryEditEntryRoute
import com.example.mytracker.nutrition.diary.DiaryEditEntryScreen
import com.example.mytracker.nutrition.diary.DiaryHistoryRoute
import com.example.mytracker.nutrition.diary.DiaryHistoryScreen
import com.example.mytracker.nutrition.diary.DiaryRoute
import com.example.mytracker.nutrition.diary.DiaryScreen
import com.example.mytracker.nutrition.food.FoodEditRoute
import com.example.mytracker.nutrition.food.FoodEditScreen
import com.example.mytracker.nutrition.library.LibraryLogRoute
import com.example.mytracker.nutrition.library.LibraryRoute
import com.example.mytracker.nutrition.library.LibraryScreen
import com.example.mytracker.nutrition.recipe.RecipeEditRoute
import com.example.mytracker.nutrition.recipe.RecipeEditScreen
import com.example.mytracker.sleep.SleepRoute
import com.example.mytracker.sleep.SleepScreen
import com.example.mytracker.sleep.SleepTagManageRoute
import com.example.mytracker.sleep.SleepTagManageScreen
import com.example.mytracker.task.TaskRoute
import com.example.mytracker.task.TaskScreen
import com.example.mytracker.weight.WeightRoute
import com.example.mytracker.weight.WeightScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = DiaryRoute,
        modifier = modifier,
    ) {
        composable<DiaryRoute> {
            DiaryScreen(
                // Adding lands in the Bibliothek, carrying this day and meal — the same screen the
                // drawer opens, only with somewhere to write to.
                onAddEntry = { epochDay, mealType ->
                    navController.navigate(LibraryLogRoute(epochDay, mealType))
                },
                onEditEntry = { entryId -> navController.navigate(DiaryEditEntryRoute(entryId)) },
                onOpenHistory = { navController.navigate(DiaryHistoryRoute) },
                // Same navigation as the drawer entry, not a plain navigate: the Bibliothek is a
                // top-level destination, and pushing one of those as if it were a detail screen
                // leaves the bottom bar unable to switch away from it again (see
                // [navigateToTopLevel]).
                onOpenLibrary = { navController.navigateToTopLevel(LibraryRoute) },
                onManageFluidQuickAdds = { navController.navigate(FluidQuickAddManageRoute) },
                onOpenDrawer = onOpenDrawer,
            )
        }
        composable<DiaryHistoryRoute> { entry ->
            DiaryHistoryScreen(onBack = { navController.popBackStackOnce(entry) })
        }
        composable<FluidRoute> {
            FluidScreen(
                onOpenDrawer = onOpenDrawer,
                onOpenTypeManagement = { navController.navigate(FluidTypeManageRoute) },
                onOpenUnitManagement = { navController.navigate(FluidUnitManageRoute) },
                onOpenQuickAddManagement = { navController.navigate(FluidQuickAddManageRoute) },
            )
        }
        composable<FluidQuickAddManageRoute> { entry ->
            FluidQuickAddManageScreen(onBack = { navController.popBackStackOnce(entry) })
        }
        composable<FluidTypeManageRoute> { entry ->
            FluidTypeManageScreen(onBack = { navController.popBackStackOnce(entry) })
        }
        composable<FluidUnitManageRoute> { entry ->
            FluidUnitManageScreen(onBack = { navController.popBackStackOnce(entry) })
        }
        composable<HabitRoute> {
            HabitScreen(onOpenDrawer = onOpenDrawer)
        }
        composable<TaskRoute> {
            TaskScreen(onOpenDrawer = onOpenDrawer)
        }
        composable<WeightRoute> {
            WeightScreen(onOpenDrawer = onOpenDrawer)
        }
        composable<MeasurementRoute> {
            MeasurementScreen(
                onOpenDrawer = onOpenDrawer,
                onOpenSiteManagement = { navController.navigate(BodySiteManageRoute) },
            )
        }
        composable<BloodPressureRoute> {
            BloodPressureScreen(onOpenDrawer = onOpenDrawer)
        }
        composable<SleepRoute> {
            SleepScreen(
                onOpenDrawer = onOpenDrawer,
                onOpenTagManagement = { navController.navigate(SleepTagManageRoute) },
            )
        }
        composable<SleepTagManageRoute> { entry ->
            SleepTagManageScreen(onBack = { navController.popBackStackOnce(entry) })
        }
        composable<BodySiteManageRoute> { entry ->
            BodySiteManageScreen(onBack = { navController.popBackStackOnce(entry) })
        }
        composable<DiaryEditEntryRoute> { entry ->
            DiaryEditEntryScreen(onDone = { navController.popBackStackOnce(entry) })
        }
        composable<LibraryRoute> {
            LibraryScreen(
                onAddFood = { navController.navigate(FoodEditRoute()) },
                onEditFood = { foodId -> navController.navigate(FoodEditRoute(foodId = foodId)) },
                onAddRecipe = { navController.navigate(RecipeEditRoute()) },
                onEditRecipe = { recipeId -> navController.navigate(RecipeEditRoute(recipeId = recipeId)) },
                onOpenDrawer = onOpenDrawer,
            )
        }
        // The same screen, reached from a meal's "+": a detail destination that Zurück returns from,
        // and the only difference is the day and meal it writes to.
        composable<LibraryLogRoute> { entry ->
            val route: LibraryLogRoute = entry.toRoute()
            LibraryScreen(
                onAddFood = { navController.navigate(FoodEditRoute()) },
                onEditFood = { foodId -> navController.navigate(FoodEditRoute(foodId = foodId)) },
                onAddRecipe = { navController.navigate(RecipeEditRoute()) },
                onEditRecipe = { recipeId -> navController.navigate(RecipeEditRoute(recipeId = recipeId)) },
                onOpenDrawer = onOpenDrawer,
                epochDay = route.epochDay,
                mealType = route.mealType,
                onBack = { navController.popBackStackOnce(entry) },
            )
        }
        composable<FoodEditRoute> { entry ->
            FoodEditScreen(onDone = { navController.popBackStackOnce(entry) })
        }
        composable<RecipeEditRoute> { entry ->
            RecipeEditScreen(onDone = { navController.popBackStackOnce(entry) })
        }
        composable<BackupRoute> {
            BackupScreen(onOpenDrawer = onOpenDrawer)
        }
        composable<AchievementsRoute> {
            AchievementsScreen(onOpenDrawer = onOpenDrawer)
        }
        composable<AnalyseRoute> {
            AnalyseScreen(onOpenDrawer = onOpenDrawer)
        }
        composable<GoalsRoute> {
            GoalsScreen(onOpenDrawer = onOpenDrawer)
        }
        composable<DayGoalsRoute> {
            DayGoalsScreen(
                onOpenDrawer = onOpenDrawer,
                // Ziele is a drawer destination too — same reason as onOpenLibrary above.
                onEditGoals = { navController.navigateToTopLevel(GoalsRoute) },
            )
        }
        composable<FitnessRoute> {
            FitnessScreen(
                onOpenHistory = { navController.navigate(TrainingHistoryRoute) },
                onOpenExercise = { exerciseId ->
                    navController.navigate(
                        StrengthExerciseDetailRoute(exerciseId = exerciseId, epochDay = DateUtils.todayEpochDay()),
                    )
                },
                onOpenCardioActivity = { activityTypeId ->
                    navController.navigate(
                        CardioActivityDetailRoute(
                            activityTypeId = activityTypeId,
                            epochDay = DateUtils.todayEpochDay(),
                        ),
                    )
                },
                onAddExercise = { navController.navigate(StrengthExerciseEditRoute()) },
                onOpenExerciseLibrary = { navController.navigate(StrengthExerciseLibraryRoute) },
                onOpenMuscleGroupLibrary = { navController.navigate(MuscleGroupManageRoute) },
                onOpenCardioActivityTypeLibrary = { navController.navigate(CardioActivityTypeManageRoute) },
                onOpenDrawer = onOpenDrawer,
            )
        }
        composable<TrainingHistoryRoute> { entry ->
            TrainingHistoryScreen(
                onBack = { navController.popBackStackOnce(entry) },
                // Navigating by (subject, day) rather than entry id, so the detail page can show
                // the right "letztes Training" comparison relative to the day being edited.
                onOpenStrengthSession = { exerciseId, epochDay ->
                    navController.navigate(StrengthExerciseDetailRoute(exerciseId = exerciseId, epochDay = epochDay))
                },
                onOpenCardioSession = { activityTypeId, epochDay, sessionId ->
                    navController.navigate(
                        CardioActivityDetailRoute(
                            activityTypeId = activityTypeId,
                            epochDay = epochDay,
                            sessionId = sessionId,
                        ),
                    )
                },
            )
        }
        composable<StrengthExerciseDetailRoute> { entry ->
            StrengthExerciseDetailScreen(onBack = { navController.popBackStackOnce(entry) })
        }
        composable<CardioActivityDetailRoute> { entry ->
            CardioActivityDetailScreen(onBack = { navController.popBackStackOnce(entry) })
        }
        composable<CardioActivityTypeManageRoute> { entry ->
            CardioActivityTypeManageScreen(onBack = { navController.popBackStackOnce(entry) })
        }
        composable<MuscleGroupManageRoute> { entry ->
            MuscleGroupManageScreen(onBack = { navController.popBackStackOnce(entry) })
        }
        composable<StrengthExerciseLibraryRoute> { entry ->
            StrengthExerciseLibraryScreen(
                onBack = { navController.popBackStackOnce(entry) },
                onAddExercise = { navController.navigate(StrengthExerciseEditRoute()) },
                onEditExercise = { exerciseId -> navController.navigate(StrengthExerciseEditRoute(exerciseId = exerciseId)) },
            )
        }
        composable<StrengthExerciseEditRoute> { entry ->
            StrengthExerciseEditScreen(onDone = { navController.popBackStackOnce(entry) })
        }
    }
}
