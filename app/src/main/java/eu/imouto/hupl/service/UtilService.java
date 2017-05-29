package eu.imouto.hupl.service;

import android.app.IntentService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import eu.imouto.hupl.R;

public class UtilService extends IntentService
{
    private static final String ACTION_CLIP = "eu.imouto.hupl.COPY_TO_CLIPBOARD";

    public UtilService()
    {
        super("UtilService");
    }

    public static Intent copyToClipboardIntent(Context context, String text)
    {
        Intent in = new Intent(context, UtilService.class);
        in.setAction(ACTION_CLIP);
        in.putExtra("CLIP_TEXT", text);
        return in;
    }

    public static void copyToClipboard(Context context, String text)
    {
        Intent in = copyToClipboardIntent(context, text);
        context.startService(in);
    }

    private void toast(final String text, final int duration)
    {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable()
        {
            @Override
            public void run()
            {
                Toast.makeText(getApplicationContext(), text, duration).show();
            }
        });
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        if (intent != null)
        {
            final String action = intent.getAction();
            if (ACTION_CLIP.equals(action))
            {
                String text = intent.getStringExtra("CLIP_TEXT");
                String strCopied = getResources().getString(R.string.toast_url_copied);
                ClipboardManager clipMgr = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData data = ClipData.newPlainText("url", text);
                clipMgr.setPrimaryClip(data);
                toast(strCopied, Toast.LENGTH_SHORT);

                //hide notification tray
                sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            }
        }
    }
}
