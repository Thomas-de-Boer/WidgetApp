package com.tdeboer.widgetapp.widgets.gen;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;

// android freezes an app without a visible screen, even in the middle of rendering.
// a running job keeps the app awake until the frames of the widget are ready
public class GenerationJobService extends JobService {
    private static final String EXTRA_WIDGET_ID = "app_widget_id";
    private static final String EXTRA_RANDOMIZE = "randomize";

    // the widget id is the job id, so a newer request for a widget replaces the older one
    static void schedule(Context context, int appWidgetId, boolean randomize) {
        PersistableBundle extras = new PersistableBundle();
        extras.putInt(EXTRA_WIDGET_ID, appWidgetId);
        extras.putBoolean(EXTRA_RANDOMIZE, randomize);
        JobInfo job = new JobInfo.Builder(appWidgetId, new ComponentName(context, GenerationJobService.class))
                .setExtras(extras)
                .setOverrideDeadline(0)
                .build();
        context.getSystemService(JobScheduler.class).schedule(job);
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        PersistableBundle extras = params.getExtras();
        WidgetProviderGen.generateNow(getApplicationContext(), extras.getInt(EXTRA_WIDGET_ID),
                extras.getBoolean(EXTRA_RANDOMIZE), () -> jobFinished(params, false));
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        // another run keeps the app awake if android stops this one before the frames are ready
        return true;
    }
}
