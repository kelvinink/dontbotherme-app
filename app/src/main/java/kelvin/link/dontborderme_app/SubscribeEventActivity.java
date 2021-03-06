package kelvin.link.dontborderme_app;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SubscribeEventActivity extends AppCompatActivity implements EventDetailDialog.EventDetailListener{
    private static final String TIMESTAMP_ZERO = "0000-00-00 00:00:00";
    private String logMessage = "SubscribeActivity:";
    private ArrayList<EventItem> eventItemArrayList = new ArrayList<>();
    private RecyclerView mRecyclerView;
    private EventItemAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private DontBorderMeWebServiceAPI webServiceAPI;
    private User user;


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    Intent main_intent = new Intent(SubscribeEventActivity.this, LocalEventActivity.class);
                    startActivity(main_intent);
                    return true;
                case R.id.navigation_sender:
                    Intent sender_intent = new Intent(SubscribeEventActivity.this, SendEventActivity.class);
                    startActivity(sender_intent);
                    return true;
                case R.id.navigation_subscriber:
                    return true;
                case R.id.navigation_me:
                    Intent settings_intent = new Intent(SubscribeEventActivity.this, MeActivity.class);
                    startActivity(settings_intent);
                    return true;
            }
            return false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscribe_event);


        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.subscribe_activity_navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        navigation.setSelectedItemId(R.id.navigation_subscriber);

        //Setting up user info
        user = UserManager.getInstance().getUser();

        //Setting up retrofit connection
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://59.149.35.12/dontborderme_webservice/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        webServiceAPI = retrofit.create(DontBorderMeWebServiceAPI.class);


        mRecyclerView = findViewById(R.id.subscribe_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mAdapter = new EventItemAdapter(eventItemArrayList);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(new EventItemAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                //TODO Set onclick event
            }

            @Override
            public void onTitleClick(int position) {
                Event event = eventItemArrayList.get(position).getEvent();
                Bundle bundle = new Bundle();
                bundle.putInt(EventDetailDialog.EXTRA_EVENT_ID, event.getEvent_id());
                bundle.putString(EventDetailDialog.EXTRA_EVENT_TITLE, event.getEvent_title());
                bundle.putString(EventDetailDialog.EXTRA_ADDRESS, event.getAddress());
                bundle.putString(EventDetailDialog.EXTRA_DESCRIPTION, event.getDescription());
                bundle.putString(EventDetailDialog.EXTRA_START_TS, event.getStart_ts());
                EventDetailDialog eventDetailDialog = new EventDetailDialog();
                eventDetailDialog.setArguments(bundle);
                eventDetailDialog.show(getSupportFragmentManager(), "Event Detail");
            }

            @Override
            public void onIconClick(int position) {
                //Do nothing
            }

            @Override
            public void onEditClick(int position) {
                //Do nothing
            }
        });



        //For delete
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT|ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Integer event_id = eventItemArrayList.get(position).getEvent().getEvent_id();
                WebServiceDAO webServiceDAO = new WebServiceDAO();
                webServiceDAO.deleteEvent(user.getUid(), event_id);
                removeItem(position);
                Toast.makeText(SubscribeEventActivity.this, "Unsubscribe", Toast.LENGTH_SHORT).show();
            }
        }).attachToRecyclerView(mRecyclerView);


    }


    @Override
    protected void onResume() {
        super.onResume();

        getSubscribeEvents(user.getUid());
    }

    public void removeItem(int position){
        eventItemArrayList.remove(position);
        mAdapter.notifyItemRemoved(position);
    }


    private void updateAdapter(List<Event> events){
        eventItemArrayList.clear();
        for(Event e: events){
            if(e.getStart_ts() != null && e.getStart_ts().equals(TIMESTAMP_ZERO)){
                e.setStart_ts(null);
            }
            eventItemArrayList.add(new EventItem(R.drawable.ic_remind, e));
        }
    }


    //Arguments required: <uid>
    private void getSubscribeEvents(String uid){
        Map<String, String> parameters = new HashMap<>();
        parameters.put("uid", uid);
        Call<List<Event>> call = webServiceAPI.getSubscribeEvents(parameters);

        //Async call
        call.enqueue(new Callback<List<Event>>() {
            @Override
            public void onResponse(Call<List<Event>> call, Response<List<Event>> response) {
                if(!response.isSuccessful()){
                    Log.i(logMessage, "getSubscribeEvents(); Response code:" + response.code());
                    return;
                }
                updateAdapter(response.body());
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Call<List<Event>> call, Throwable t){
                Log.i(logMessage, "getSubscribeEvents(); Fialure message: " + t.getMessage());
            }
        });
    }


    //Arguments required: <uid>, <event_id>
    private void deleteEvent(String uid, Integer event_id){
        Map<String, String> parameters = new HashMap<>();
        parameters.put("uid", uid);
        parameters.put("event_id", String.valueOf(event_id));
        Call<Void> call = webServiceAPI.deleteEvent(parameters);

        //Async call
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if(!response.isSuccessful()){
                    Log.i(logMessage, "_deleteEvent(); Response code:" + response.code());
                    return;
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t){
                Log.i(logMessage, "_deleteEvent(); Fialure message: " + t.getMessage());
            }
        });
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.subscribe_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.scan_qr_code:
                startActivity(new Intent(getApplicationContext(),ScanCodeActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onResult() {
        //EventDialogListener handle result
    }
}
