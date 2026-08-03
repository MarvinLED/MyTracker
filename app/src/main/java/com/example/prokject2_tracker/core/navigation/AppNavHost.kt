package com.example.prokject2_tracker.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.ui.Modifier
import com.example.prokject2_tracker.analyse.AnalyseRoute
import com.example.prokject2_tracker.analyse.AnalyseScreen
import com.example.prokject2_tracker.bloodpressure.BloodPressureRoute
import com.example.prokject2_tracker.bloodpressure.BloodPressureScreen
import com.example.prokject2_tracker.core.util.DateUtils
import com.example.prokject2_tracker.fitness.FitnessRoute
import com.example.prokject2_tracker.fitness.FitnessScreen
import com.example.prokject2_tracker.fitness.TrainingHistoryRoute
import com.example.prokject2_tracker.fitness.TrainingHistoryScreen
import com.example.prokject2_tracker.fitness.cardio.CardioActivityDetailRoute
import com.example.prokject2_tracker.fitness.cardio.CardioActivityDetailScreen
import com.example.prokject2_tracker.fitness.cardio.CardioActivityTypeManageRoute
import com.example.prokject2_tracker.fitness.cardio.CardioActivityTypeManageScreen
import com.example.prokject2_tracker.fitness.strength.MuscleGroupManageRoute
import com.example.prokject2_tracker.fitness.strength.MuscleGroupManageScreen
import com.example.prokject2_tracker.fitness.strength.StrengthExerciseDetailRoute
import com.example.prokject2_tracker.fitness.strength.StrengthExerciseDetailScreen
import com.example.prokject2_tracker.fitness.strength.StrengthExerciseEditRoute
import com.example.prokject2_tracker.fitness.strength.StrengthExerciseEditScreen
import com.example.prokject2_tracker.fitness.strength.StrengthExerciseLibraryRoute
import com.example.prokject2_tracker.fitness.strength.StrengthExerciseLibraryScreen
import com.example.prokject2_tracker.fluid.FluidQuickAddManageRoute
import com.example.prokject2_tracker.fluid.FluidQuickAddManageScreen
import com.example.prokject2_tracker.fluid.FluidRoute
import com.example.prokject2_tracker.fluid.FluidScreen
import com.example.prokject2_tracker.fluid.FluidTypeManageRoute
import com.example.prokject2_tracker.fluid.FluidTypeManageScreen
import com.example.prokject2_tracker.fluid.FluidUnitManageRoute
import com.example.prokject2_tracker.fluid.FluidUnitManageScreen
import com.example.prokject2_tracker.goals.DayGoalsRoute
import com.example.prokject2_tracker.goals.DayGoalsScreen
import com.example.prokject2_tracker.goals.GoalsRoute
import com.example.prokject2_tracker.goals.GoalsScreen
import com.example.prokject2_tracker.habit.HabitRoute
import com.example.prokject2_tracker.habit.HabitScreen
import com.example.prokject2_tracker.measurement.BodySiteManageRoute
import com.example.prokject2_tracker.measurement.BodySiteManageScreen
import com.example.prokject2_tracker.measurement.MeasurementRoute
import com.example.prokject2_tracker.measurement.MeasurementScreen
import com.example.prokject2_tracker.nutrition.diary.DiaryAddEntryRoute
import com.example.prokject2_tracker.nutrition.diary.DiaryAddEntryScreen
import com.example.prokject2_tracker.nutrition.diary.DiaryEditEntryRoute
import com.example.prokject2_tracker.nutrition.diary.DiaryEditEntryScreen
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
import com.example.prokject2_tracker.sleep.SleepRoute
import com.example.prokject2_tracker.sleep.SleepScreen
import com.example.prokject2_tracker.sleep.SleepTagManageRoute
import com.example.prokject2_tracker.sleep.SleepTagManageScreen
import com.example.prokject2_tracker.task.TaskRoute
import com.example.prokject2_tracker.task.TaskScreen
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
        startDestination = DiaryRoute,
        modifier = modifier,
    ) {
        composable<DiaryRoute> {
            DiaryScreen(
                onAddEntry = { epochDay, mealType ->
                    navController.navigate(DiaryAddEntryRoute(epochDay, mealType))
                },
                onEditEntry = { entryId -> navController.navigate(DiaryEditEntryRoute(entryId)) },
                // Same navigation as the drawer entry, not a plain navigate: the Bibliothek is a
                // top-level destination, and pushing one of those as if it were a detail screen
                // leaves the bottom bar unable to switch away from it again (see
                // [navigateToTopLevel]).
                onOpenLibrary = { navController.navigateToTopLevel(LibraryRoute) },
                onManageFluidQuickAdds = { navController.navigate(FluidQuickAddManageRoute) },
                onOpenDrawer = onOpenDrawer,
            )
        }
        composable<FluidRoute> {
            FluidScreen(
                onOpenDrawer = onOpenDrawer,
                onOpenTypeManagement = { navController.navigate(FluidTypeManageRoute) },
                onOpenUnitManagement = { navController.navigate(FluidUnitManageRoute) },
                onOpenQuickAddManagement = { navController.navigate(FluidQuickAddManageRoute) },
            )
        }
        composable<FluidQuickAddManageRoute> {
            FluidQuickAddManageScreen(onBack = { navController.popBackStack() })
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
        composable<SleepTagManageRoute> {
            SleepTagManageScreen(onBack = { navController.popBackStack() })
        }
        composable<BodySiteManageRoute> {
            BodySiteManageScreen(onBack = { navController.popBackStack() })
        }
        composable<DiaryAddEntryRoute> {
            DiaryAddEntryScreen(
                onDone = { navController.popBackStack() },
                onCreateFood = { navController.navigate(FoodEditRoute()) },
                onCreateRecipe = { navController.navigate(RecipeEditRoute()) },
            )
        }
        composable<DiaryEditEntryRoute> {
            DiaryEditEntryScreen(onDone = { navController.popBackStack() })
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
        composable<TrainingHistoryRoute> {
            TrainingHistoryScreen(
                onBack = { navController.popBackStack() },
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
        composable<StrengthExerciseDetailRoute> {
            StrengthExerciseDetailScreen(onBack = { navController.popBackStack() })
        }
        composable<CardioActivityDetailRoute> {
            CardioActivityDetailScreen(onBack = { navController.popBackStack() })
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
