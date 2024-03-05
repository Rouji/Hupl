package eu.imouto.hupl.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.MenuItem;
import android.widget.RelativeLayout;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import eu.imouto.hupl.R;

public abstract class DrawerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener
{
    private DrawerLayout drawer;
    private Toolbar toolbar;
    private NavigationView navigationView;

    private static final SparseArray<Class> navActivityMap = createMap();
    private static SparseArray<Class> createMap()
    {
        SparseArray<Class> m = new SparseArray<>();
        m.put(R.id.nav_uploaders, ChooseUploaderActivity.class);
        m.put(R.id.nav_history, HistoryActivity.class);
        m.put(R.id.nav_settings, GlobalSettingsActivity.class);
        m.put(R.id.nav_about, AboutActivity.class);
        //m.put(R.id.nav_feedback, FeedbackActivity.class);
        return m;
    }

    private void setCheckedNavItem()
    {
        //TODO: use a bidirectional map or something, this is too ugly
        for (int i=0; i<navActivityMap.size(); i++)
        {
            if (navActivityMap.valueAt(i) == this.getClass())
            {
                navigationView.setCheckedItem(navActivityMap.keyAt(i));
                return;
            }
        }
    }


    abstract @LayoutRes int onInflateContent();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawer);

        RelativeLayout content = (RelativeLayout) findViewById(R.id.drawer_activity_content);
        getLayoutInflater().inflate(onInflateContent(), content, true);

        toolbar = (Toolbar) findViewById(R.id.toolbar);

        drawer = (DrawerLayout) findViewById(R.id.drawer_activity_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, 0, 0);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    protected void onPostResume()
    {
        super.onPostResume();
        setCheckedNavItem();
    }

    @Override
    public void onBackPressed()
    {
        if (drawer.isDrawerOpen(GravityCompat.START))
        {
            drawer.closeDrawer(GravityCompat.START);
        }
        else
        {
            super.onBackPressed();
        }
    }

    @Override
    public void setTitle(CharSequence title)
    {
        super.setTitle(title);
        toolbar.setTitle(title);
    }

    /**
     * handles navigation drawer item clicks
     */
    @Override
    public boolean onNavigationItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        drawer.closeDrawer(GravityCompat.START);

        Class c = navActivityMap.get(id);
        Class t = this.getClass();

        if (t != c)
        {
            Intent in = new Intent(getApplicationContext(), c);
            in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(in);
        }

        return true;
    }
}
