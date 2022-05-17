package com.example.scoreviewer;

import com.example.scoreviewer.ble.peripheral.Communications;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
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
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class Cornhole extends Fragment {
    private static final String TAG = Cornhole.class.getSimpleName();

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

    Button AddOneT1;
    Button AddTwoT1;
    Button AddThreeT1;
    Button AddFourT1;
    Button AddFiveT1;
    Button AddSixT1;
    Button AddSevemT1;
    Button AddEightT1;
    Button AddNineT1;
    Button AddTenT1;
    Button AddElevenT1;
    Button AddTwelveT1;
    Button SubOneT1;
    Button SubTwoT1;
    Button Over21T1;

    Button AddOneT2;
    Button AddTwoT2;
    Button AddThreeT2;
    Button AddFourT2;
    Button AddFiveT2;
    Button AddSixT2;
    Button AddSevemT2;
    Button AddEightT2;
    Button AddNineT2;
    Button AddTenT2;
    Button AddElevenT2;
    Button AddTwelveT2;
    Button SubOneT2;
    Button SubTwoT2;
    Button Over21T2;

    Button ResetScores;
    // UI
    private TextView mT1ScoreView;
    private TextView mT2ScoreView;


    // Data
    private Communications mCommunications;
    private CornholeListener mListener;
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
    public static Cornhole newInstance(Communications _mCommunications) {
        Cornhole fragment = new Cornhole(_mCommunications);
        return fragment;
    }

    public Cornhole(Communications _mCommunications) {
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
        return inflater.inflate(R.layout.fragment_cornhole, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)  {
        super.onViewCreated(view, savedInstanceState);

        // Set title
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(R.string.gamecornhole);
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }

        AddOneT1= view.findViewById(R.id.CH_Plus1Team1);
        AddTwoT1=view.findViewById(R.id.CH_Plus2Team1);
        AddThreeT1=view.findViewById(R.id.CH_Plus3Team1);
        AddFourT1=view.findViewById(R.id.CH_Plus4Team1);
        AddFiveT1=view.findViewById(R.id.CH_Plus5Team1);
        AddSixT1=view.findViewById(R.id.CH_Plus6Team1);
        AddSevemT1=view.findViewById(R.id.CH_Plus7Team1);
        AddEightT1=view.findViewById(R.id.CH_Plus8Team1);
        AddNineT1=view.findViewById(R.id.CH_Plus9Team1);
        AddTenT1=view.findViewById(R.id.CH_Plus10Team1);
        AddElevenT1= view.findViewById(R.id.CH_Plus11Team1);
        AddTwelveT1=view.findViewById(R.id.CH_Plus12Team1);
        SubOneT1=view.findViewById(R.id.CH_Sub1Team1);
        SubTwoT1=view.findViewById(R.id.CH_Sub2Team1);
        Over21T1=view.findViewById(R.id.CH_Over21Team1);
        
        AddOneT2=view.findViewById(R.id.CH_Plus1Team2);
        AddTwoT2=view.findViewById(R.id.CH_Plus2Team2);
        AddThreeT2=view.findViewById(R.id.CH_Plus3Team2);
        AddFourT2=view.findViewById(R.id.CH_Plus4Team2);
        AddFiveT2=view.findViewById(R.id.CH_Plus5Team2);
        AddSixT2=view.findViewById(R.id.CH_Plus6Team2);
        AddSevemT2=view.findViewById(R.id.CH_Plus7Team2);
        AddEightT2=view.findViewById(R.id.CH_Plus8Team2);
        AddNineT2=view.findViewById(R.id.CH_Plus9Team2);
        AddTenT2=view.findViewById(R.id.CH_Plus10Team2);
        AddElevenT2=view.findViewById(R.id.CH_Plus11Team2);
        AddTwelveT2=view.findViewById(R.id.CH_Plus12Team2);
        SubOneT2=view.findViewById(R.id.CH_Sub1Team2);
        SubTwoT2=view.findViewById(R.id.CH_Sub2Team2);
        Over21T2=view.findViewById(R.id.CH_Over21Team2);

        ResetScores=view.findViewById(R.id.CH_resetscores);

        mT1ScoreView=view.findViewById(R.id.Team1Score);
        mT2ScoreView=view.findViewById(R.id.Team2Score);

        mCommunications.sendScoreUpdate(Communications.requestScoreupdate);
        mT1ScoreView.setText(String.format(getString(R.string.team_score_format), mCommunications.getT1Score()));
        mT2ScoreView.setText(String.format(getString(R.string.team_score_format), mCommunications.getT2Score()));

        Log.d(TAG, "Ready");
        AddOneT1.setOnClickListener(v -> mCommunications.sendScoreUpdate(Communications.addT1));
        AddTwoT1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for (int i = 0; i < 2; i++) {
                    mCommunications.sendScoreUpdate(Communications.addT1);
                }
            }
        });
        AddThreeT1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<3;i++) {
                    mCommunications.sendScoreUpdate(Communications.addT1);
                }
            }
        });
        AddFourT1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<4;i++) {
                    mCommunications.sendScoreUpdate(Communications.addT1);
                }
            }
        });
        AddFiveT1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<5;i++) {
                    mCommunications.sendScoreUpdate(Communications.addT1);
                }
            }
        });
        AddSixT1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<6;i++) {
                    mCommunications.sendScoreUpdate(Communications.addT1);
                }
            }
        });
        AddSevemT1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<7;i++) {
                    mCommunications.sendScoreUpdate(Communications.addT1);
                }
            }
        });
        AddEightT1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<8;i++) {
                    mCommunications.sendScoreUpdate(Communications.addT1);
                }
            }
        });
        AddNineT1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<9;i++) {
                    mCommunications.sendScoreUpdate(Communications.addT1);
                }
            }
        });
        AddTenT1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<10;i++) {
                    mCommunications.sendScoreUpdate(Communications.addT1);
                }
            }
        });
        AddElevenT1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<11;i++) {
                    mCommunications.sendScoreUpdate(Communications.addT1);
                }
            }
        });
        AddTwelveT1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<12;i++) {
                    mCommunications.sendScoreUpdate(Communications.addT1);
                }
            }
        });
        SubOneT1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<1;i++) {
                    mCommunications.sendScoreUpdate(Communications.subT1);
                }
            }
        });
        SubTwoT1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<2;i++) {
                    mCommunications.sendScoreUpdate(Communications.subT1);
                }
            }
        });
        Over21T1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCommunications.sendScoreUpdate(Communications.set15T1);
            }
        });

        AddOneT2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<1;i++) {
                    mCommunications.sendScoreUpdate(Communications.addT2);
                }
            }
        });
        AddTwoT2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<2;i++) {
                    mCommunications.sendScoreUpdate(Communications.addT2);
                }
            }
        });
        AddThreeT2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<3;i++) {
                    mCommunications.sendScoreUpdate(Communications.addT2);
                }
            }
        });
        AddFourT2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<4;i++) {
                    mCommunications.sendScoreUpdate(Communications.addT2);
                }
            }
        });
        AddFiveT2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<5;i++) {
                    mCommunications.sendScoreUpdate(Communications.addT2);
                }
            }
        });
        AddSixT2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<6;i++) {
                    mCommunications.sendScoreUpdate(Communications.addT2);
                }
            }
        });
        AddSevemT2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<7;i++) {
                    mCommunications.sendScoreUpdate(Communications.addT2);
                }
            }
        });
        AddEightT2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<8;i++) {
                    mCommunications.sendScoreUpdate(Communications.addT2);
                }
            }
        });
        AddNineT2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<9;i++) {
                    mCommunications.sendScoreUpdate(Communications.addT2);
                }
            }
        });
        AddTenT2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<10;i++) {
                    mCommunications.sendScoreUpdate(Communications.addT2);
                }
            }
        });
        AddElevenT2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<11;i++) {
                    mCommunications.sendScoreUpdate(Communications.addT2);
                }
            }
        });
        AddTwelveT2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<12;i++) {
                    mCommunications.sendScoreUpdate(Communications.addT2);
                }
            }
        });   
        SubOneT2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<1;i++) {
                    mCommunications.sendScoreUpdate(Communications.subT2);
                }
            }
        });
        SubTwoT2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<2;i++) {
                    mCommunications.sendScoreUpdate(Communications.subT2);
                }
            }
        });
        Over21T2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCommunications.sendScoreUpdate(Communications.set15T2);
            }
        });
        ResetScores.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCommunications.sendScoreUpdate(Communications.resetAll);
            }
        });
    }
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof CornholeListener) {
            mListener = (CornholeListener) context;
        } else if (getTargetFragment() instanceof CornholeListener) {
            mListener = (CornholeListener) getTargetFragment();
        } else {
            throw new RuntimeException(context.toString() + " must implement CornholeListener");
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

    // region CornholeListener
    public interface CornholeListener {
        void onSendControllerPadButtonStatus(int tag, boolean isPressed);
    }
    // endregion
}
