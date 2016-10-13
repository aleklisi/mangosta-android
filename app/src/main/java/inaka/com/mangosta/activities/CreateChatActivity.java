package inaka.com.mangosta.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.nanotasks.BackgroundWork;
import com.nanotasks.Completion;
import com.nanotasks.Tasks;

import org.jivesoftware.smackx.muc.MultiUserChat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import inaka.com.mangosta.R;
import inaka.com.mangosta.adapters.UsersListAdapter;
import inaka.com.mangosta.chat.RoomManager;
import inaka.com.mangosta.models.User;
import inaka.com.mangosta.utils.NavigateToChat;
import inaka.com.mangosta.utils.Preferences;
import inaka.com.mangosta.utils.UserEvent;
import inaka.com.mangosta.xmpp.XMPPUtils;
import inaka.com.mangosta.xmpp.muclight.MultiUserChatLight;

public class CreateChatActivity extends BaseActivity {

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.createChatSearchUserButton)
    ImageButton createChatSearchUserButton;

    @Bind(R.id.createChatSearchUserEditText)
    EditText createChatSearchUserEditText;

    @Bind(R.id.createChatSearchUserProgressBar)
    ProgressBar createChatSearchUserProgressBar;

    @Bind(R.id.createChatSearchResultRecyclerView)
    RecyclerView createChatSearchResultRecyclerView;

    @Bind(R.id.createChatMembersRecyclerView)
    RecyclerView createChatMembersRecyclerView;

    @Bind(R.id.createChatFloatingButton)
    FloatingActionButton createChatFloatingButton;

    private List<User> mSearchUsers;
    private List<User> mMemberUsers;

    UsersListAdapter mMembersAdapter;
    UsersListAdapter mSearchAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_chat);

        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);

        LinearLayoutManager layoutManagerSearch = new LinearLayoutManager(this);
        createChatSearchResultRecyclerView.setHasFixedSize(true);
        createChatSearchResultRecyclerView.setLayoutManager(layoutManagerSearch);

        LinearLayoutManager layoutManagerMembers = new LinearLayoutManager(this);
        createChatMembersRecyclerView.setHasFixedSize(true);
        createChatMembersRecyclerView.setLayoutManager(layoutManagerMembers);

        mMemberUsers = new ArrayList<>();
        mSearchUsers = new ArrayList<>();

        mSearchAdapter = new UsersListAdapter(this, mSearchUsers, true, false);
        mMembersAdapter = new UsersListAdapter(this, mMemberUsers, false, true);

        createChatMembersRecyclerView.setAdapter(mMembersAdapter);
        createChatSearchResultRecyclerView.setAdapter(mSearchAdapter);

        createChatSearchUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createChatSearchUserButton.setVisibility(View.GONE);
                createChatSearchUserProgressBar.setVisibility(View.VISIBLE);
                final String user = createChatSearchUserEditText.getText().toString();
                searchUserBackgroundTask(user);
            }
        });

        createChatFloatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createChat(mMemberUsers);
            }
        });

    }

    private void searchUserBackgroundTask(final String user) {
        Tasks.executeInBackground(CreateChatActivity.this, new BackgroundWork<String>() {
            @Override
            public String doInBackground() throws Exception {
                if (XMPPUtils.userExists(user)) {
                    obtainUser(user);
                } else {
                    showInviteDialog(user);
                }
                return user;
            }
        }, new Completion<String>() {
            @Override
            public void onSuccess(Context context, String userName) {
                createChatSearchUserProgressBar.setVisibility(View.GONE);
                createChatSearchUserButton.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(Context context, Exception e) {
                Toast.makeText(context, getString(R.string.error), Toast.LENGTH_SHORT).show();
                createChatSearchUserProgressBar.setVisibility(View.GONE);
                createChatSearchUserButton.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                finish();
                break;
        }

        return true;
    }

    private void showInviteDialog(String user) {
        final String message = String.format(Locale.getDefault(), getString(R.string.user_doesnt_exist), user);

        CreateChatActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(CreateChatActivity.this)
                        .setTitle(getString(R.string.invite_to_gistoid))
                        .setMessage(message)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // yes
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // no
                            }
                        })
                        .show();
            }
        });
    }

    private void obtainUser(final String userName) {
        Preferences preferences = Preferences.getInstance();

        User user = new User();
        user.setLogin(userName);


        mSearchUsers.clear();
        mSearchUsers.add(user);
        CreateChatActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSearchAdapter.notifyDataSetChanged();
            }
        });

    }

    // receives events from EventBus
    public void onEvent(UserEvent userEvent) {
        User user = userEvent.getUser();

        switch (userEvent.getType()) {

            case ADD_USER:
                if (!userInList(user, mMemberUsers)) {
                    mMemberUsers.add(user);
                    mMembersAdapter.notifyDataSetChanged();
                    mSearchUsers.clear();
                    mSearchAdapter.notifyDataSetChanged();
                }
                break;

            case REMOVE_USER:
                mMemberUsers.remove(user);
                mMembersAdapter.notifyDataSetChanged();
                break;
        }
    }

    private boolean userInList(User user, List<User> list) {
        boolean userFound = false;
        for (User anUser : list) {
            if (anUser.getLogin().equals(user.getLogin())) {
                userFound = true;
            }
        }
        return userFound;
    }

    private void createChat(List<User> memberUsers) {

        if (memberUsers.isEmpty()) {
            Toast.makeText(this, getString(R.string.add_people_to_create_chat), Toast.LENGTH_LONG).show();

        } else if (memberUsers.size() == 1) {   // 1 to 1 chat
            String chatJid = RoomManager.createCommonChat(memberUsers.get(0));
            NavigateToChat.go(chatJid, String.format(Locale.getDefault(), getString(R.string.chat_with), XMPPUtils.fromJIDToUserName(chatJid)), this);

        } else {    // muc or muc light
            showRoomNameDialog(memberUsers);
        }

    }

    private void showRoomNameDialog(final List<User> memberUsers) {

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        final EditText roomNameEditText = new EditText(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(10, 0, 10, 0);
        roomNameEditText.setLayoutParams(lp);

        linearLayout.addView(roomNameEditText);

        final RadioGroup radioGroup = new RadioGroup(this);

        final RadioButton radioButtonMUCLight = new RadioButton(this);
        radioButtonMUCLight.setText(getString(R.string.muc_light_chat_type));
        radioGroup.addView(radioButtonMUCLight);

        final RadioButton radioButtonMUC = new RadioButton(this);
        radioButtonMUC.setText(getString(R.string.muc_chat_type));
        radioGroup.addView(radioButtonMUC);

        linearLayout.addView(radioGroup);

        radioGroup.check(radioButtonMUCLight.getId());

        new AlertDialog.Builder(CreateChatActivity.this)
                .setTitle(getString(R.string.room_name))
                .setMessage(getString(R.string.enter_room_name))
                .setView(linearLayout)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String chatName = roomNameEditText.getText().toString();
                        String nickName = XMPPUtils.fromJIDToUserName(Preferences.getInstance().getUserXMPPJid());

                        int radioButtonId = radioGroup.getCheckedRadioButtonId();

                        if (radioButtonId == radioButtonMUCLight.getId()) {
                            MultiUserChatLight multiUserChatLight = RoomManager.createMUCLight(memberUsers, chatName);

                            if (multiUserChatLight != null) {
                                NavigateToChat.go(multiUserChatLight.getRoom().toString(), chatName, CreateChatActivity.this);
                            } else {
                                Toast.makeText(CreateChatActivity.this, getString(R.string.error_create_chat), Toast.LENGTH_SHORT).show();
                            }

                        }

                        if (radioButtonId == radioButtonMUC.getId()) {
                            MultiUserChat multiUserChat = RoomManager.createMUC(memberUsers, chatName, nickName);

                            if (multiUserChat != null) {
                                NavigateToChat.go(multiUserChat.getRoom().toString(), chatName, CreateChatActivity.this);
                            } else {
                                Toast.makeText(CreateChatActivity.this, getString(R.string.error_create_chat), Toast.LENGTH_SHORT).show();
                            }
                        }

                    }
                })
                .show();

    }

}