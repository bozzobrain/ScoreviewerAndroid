package com.example.scoreviewer;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.scoreviewer.ble.peripheral.Communications;

public class Kickball extends Fragment {
    private static final String TAG = Kickball.class.getSimpleName();

    // UI TextBuffer (refreshing the text buffer is managed with a timer because a lot of changes can arrive really fast and could stall the main thread)
    private Handler mUIRefreshTimerHandler = new Handler();
    private Runnable mUIRefreshTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isUITimerRunning) {
                updateTextDataUI();
                // Log.d(TAG, "updateDataUI");
                mUIRefreshTimerHandler.postDelayed(this, 200);
            }
        }
    };

    private boolean isUITimerRunning = false;

    Button Plus1T1;
    Button Sub1T1;
    Button Plus1T2;
    Button Sub1T2;


    Button ResetScores;
    // UI
    private TextView mT1ScoreView;
    private TextView mT2ScoreView;


    // Data
    private Communications mCommunications;
    private Kickball.KickballListener mListener;
    private volatile StringBuilder mDataBuffer = new StringBuilder();
    View.OnTouchListener mPadButtonTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            final int tag = Integer.valueOf((String) view.getTag());
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                view.setPressed(true);
                mListener.onSendControllerPadButtonStatus(tag, true);
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                view.setPressed(false);
                mListener.onSendControllerPadButtonStatus(tag, false);
                view.performClick();
                return true;
            }
            return false;
        }
    };


    // region Lifecycle
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static Kickball newInstance(Communications _mCommunications) {
        Kickball fragment = new Kickball(_mCommunications);
        return fragment;
    }

    public Kickball(Communications _mCommunications) {
        mCommunications=_mCommunications;
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_kickball, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)  {
        super.onViewCreated(view, savedInstanceState);

        // Set title
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(R.string.gamekickball);
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }

        Plus1T1=view.findViewById(R.id.KB_Plus1Team1);
        Sub1T1=view.findViewById(R.id.KB_Sub1Team1);

        Plus1T2=view.findViewById(R.id.KB_Plus1Team2);
        Sub1T2=view.findViewById(R.id.KB_Sub1Team2);


        ResetScores=view.findViewById(R.id.KB_resetscores);

        mT1ScoreView=view.findViewById(R.id.Team1ScoreKB);
        mT2ScoreView=view.findViewById(R.id.Team2ScoreKB);

        mCommunications.sendScoreUpdate(Communications.requestScoreupdate);
        mT1ScoreView.setText(String.format(getString(R.string.team_score_format), mCommunications.getT1Score()));
        mT2ScoreView.setText(String.format(getString(R.string.team_score_format), mCommunications.getT2Score()));

        Log.d(TAG, "Ready");
        Plus1T1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCommunications.sendScoreUpdate(mCommunications.addT1);
            }
        });
        Sub1T1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCommunications.sendScoreUpdate(mCommunications.subT1);
            }
        });
        Plus1T2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCommunications.sendScoreUpdate(mCommunications.addT2);
            }
        });
        Sub1T2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCommunications.sendScoreUpdate(mCommunications.subT2);
            }
        });

        ResetScores.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCommunications.sendScoreUpdate(mCommunications.resetAll);
            }
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Kickball.KickballListener) {
            mListener = (Kickball.KickballListener) context;
        } else if (getTargetFragment() instanceof Kickball.KickballListener) {
            mListener = (Kickball.KickballListener) getTargetFragment();
        } else {
            throw new RuntimeException(context.toString() + " must implement KickballListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh timer
        isUITimerRunning = true;
        mUIRefreshTimerHandler.postDelayed(mUIRefreshTimerRunnable, 0);
    }

    @Override
    public void onPause() {
        isUITimerRunning = false;
        mUIRefreshTimerHandler.removeCallbacksAndMessages(mUIRefreshTimerRunnable);
        super.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_help, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        FragmentActivity activity = getActivity();

        switch (item.getItemId()) {
            case R.id.action_help:
                if (activity != null) {
                    FragmentManager fragmentManager = activity.getSupportFragmentManager();
                    if (fragmentManager != null) {
                        CommonHelpFragment helpFragment = CommonHelpFragment.newInstance(getString(R.string.controlpad_help_title), getString(R.string.controlpad_help_text));
                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction()
                                .replace(R.id.contentLayout, helpFragment, "Help");
                        fragmentTransaction.addToBackStack(null);
                        fragmentTransaction.commit();
                    }
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // endregion
    // region UI



    private synchronized void updateTextDataUI() {
        mT1ScoreView.setText(String.format(getString(R.string.team_score_format), mCommunications.getT1Score()));
        mT2ScoreView.setText(String.format(getString(R.string.team_score_format), mCommunications.getT2Score()));
    }
    // endregion

    // region KickballListener
    public interface KickballListener {
        void onSendControllerPadButtonStatus(int tag, boolean isPressed);
    }
    // endregion


}
