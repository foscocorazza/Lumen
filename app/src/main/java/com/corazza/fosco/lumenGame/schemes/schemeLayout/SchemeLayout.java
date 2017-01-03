package com.corazza.fosco.lumenGame.schemes.schemeLayout;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.corazza.fosco.lumenGame.activities.MainActivity;
import com.corazza.fosco.lumenGame.MainThread;
import com.corazza.fosco.lumenGame.activities.MenuActivity;
import com.corazza.fosco.lumenGame.R;
import com.corazza.fosco.lumenGame.activities.TitleActivity;
import com.corazza.fosco.lumenGame.gameObjects.Background;
import com.corazza.fosco.lumenGame.gameObjects.Bulb;
import com.corazza.fosco.lumenGame.gameObjects.SegmentCreatorListener;
import com.corazza.fosco.lumenGame.gameObjects.huds.Button;
import com.corazza.fosco.lumenGame.gameObjects.ResultView;
import com.corazza.fosco.lumenGame.gameObjects.Grid;
import com.corazza.fosco.lumenGame.gameObjects.huds.Hud;
import com.corazza.fosco.lumenGame.gameObjects.Lumen;
import com.corazza.fosco.lumenGame.gameObjects.Star;
import com.corazza.fosco.lumenGame.gameObjects.huds.ResultHud;
import com.corazza.fosco.lumenGame.gameObjects.obstacles.Deflector;
import com.corazza.fosco.lumenGame.gameObjects.obstacles.Destructor;
import com.corazza.fosco.lumenGame.gameObjects.obstacles.Obstacle;
import com.corazza.fosco.lumenGame.gameObjects.obstacles.Obstructor;
import com.corazza.fosco.lumenGame.geometry.Radical;
import com.corazza.fosco.lumenGame.geometry.dots.Dot;
import com.corazza.fosco.lumenGame.geometry.dots.PixelDot;
import com.corazza.fosco.lumenGame.helpers.AnimType;
import com.corazza.fosco.lumenGame.geometry.Line;
import com.corazza.fosco.lumenGame.geometry.Path;
import com.corazza.fosco.lumenGame.geometry.Segment;
import com.corazza.fosco.lumenGame.helpers.Consts;
import com.corazza.fosco.lumenGame.helpers.MessagesHelper;
import com.corazza.fosco.lumenGame.helpers.Phase;
import com.corazza.fosco.lumenGame.helpers.Utils;
import com.corazza.fosco.lumenGame.savemanager.SaveFileManager;
import com.corazza.fosco.lumenGame.savemanager.SchemeResult;
import com.corazza.fosco.lumenGame.schemes.DList;
import com.corazza.fosco.lumenGame.schemes.SchemeLayoutDrawable;
import com.corazza.fosco.lumenGame.schemes.SchemeToast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;

public class SchemeLayout extends SurfaceView implements SurfaceHolder.Callback, Lumen.SchemeLayoutListener, Button.ButtonListener {


    private static final int MAX_NEED = 10;
    protected MainThread thread;

    // Oggetti di gioco.

    public Hud         hud1;
    public Dot         spnt;
    public Path        path;
    public Grid        grid;
    public Background  bckg;
    public SchemeToast tost;

    public ResultHud   hud2;
    public ResultView  ends;

    public DList<Bulb> blbs = new DList<>();
    public DList<Star> strs = new DList<>();
    public DList<Lumen> lums = new DList<>();
    public DList<Obstacle> obst = new DList<>();

    private HashSet<SchemeLayoutDrawable> drawableCollection = new HashSet<>();


    // Variabili di gioco.

    protected boolean touchEnabled    = true;
    private boolean drawElements    = true;
    protected Phase phase = Phase.USER_PLAYING;
    protected String  code;
    private Radical minDist = Radical.Zero;
    protected String name;
    private int satisfiedBulbs = 0;
    private boolean showLength = true;

    // Costruttori e Inizializzatori.
    public SchemeLayout(Context activity) {
        super(activity);
        init();
    }

    private void init() {
        getHolder().addCallback(this);
        phase = Phase.USER_PLAYING;
        thread = new MainThread(getHolder(), this);
        bckg = new Background();
        hud1 = new Hud(this);
        hud2 = new ResultHud(this);
        ends = new ResultView(getContext());

        collect(hud1, bckg, obst, strs, lums, hud2, ends);

        setOnTouchListener(getSegmentCreator());
    }

    protected void collect(SchemeLayoutDrawable... drawables) {
        Collections.addAll(drawableCollection, drawables);
    }

    protected void uncollect(SchemeLayoutDrawable drawables) {
        drawableCollection.remove(drawables);
    }

    // Getter and Setter
    public void  setPath(Path path) {
        this.path = path;
        collect(path);
    }

    public Lumen getMainLumen() {
        if(lums == null || lums.size() == 0) return null;
        return lums.get(0);
    }
    public void  setMainLumen(Dot p) {
        phase = Phase.USER_PLAYING;
        lums.clear();
        lums.add(new Lumen(getContext(), this, p));
        spnt = p;
    }

    public void setGrid(Collection<Dot> points) {
        grid = new Grid(points);
        ends.setGrid(grid);
        collect(grid);
    }

    public void setGrid(Grid.FillType fillType) {
        grid = new Grid(fillType);
        ends.setGrid(grid);
        collect(grid);
    }

    public void setGrid(Grid grid) {
        this.grid = grid;
        ends.setGrid(grid);
        grid.renew();
        collect(grid);
    }

    public void setDrawElements(boolean value) { this.drawElements = value; }


    public void putObstacle(Obstacle obstacle){
        if(obst == null) obst = new DList<>();
        obst.add(obstacle);
        obstacle.startAnimation();
    }


    public void putDestructor(Dot p1, Dot p2){
        putObstacle(new Destructor(p1,p2));
    }
    public void putDeflector(Dot p1, Dot p2){
        putObstacle(new Deflector(p1,p2));
    }
    public void putObstructor(Dot p1, Dot p2){
        putObstacle(new Obstructor(p1,p2));
    }

    public void putObstructors(List<Pair<Dot, Dot>> dots){
        if(dots != null)
            for(Pair<Dot, Dot> pair : dots )
                putObstructor(pair.first, pair.second);
    }

    public void putDestructors(List<Pair<Dot, Dot>> dots) {
        if(dots != null)
            for(Pair<Dot, Dot> pair : dots )
                putDestructor(pair.first, pair.second);
    }

    public void putDeflectors(List<Pair<Dot, Dot>> dots) {
        if(dots != null)
            for(Pair<Dot, Dot> pair : dots )
                putDeflector(pair.first, pair.second);
    }



    public List<Obstructor> getObstructors(){
        List<Obstructor> r = new ArrayList<>();
        if(obst != null)
            for(Obstacle o : obst){
                if(o instanceof Obstructor) r.add((Obstructor) o);
            }
        return r;
    }

    // Draw
    @Override
    protected void onDraw(Canvas canvas) {}

    // Gestione del movimento dei Lumen
    public void start() {
        if(phase == Phase.USER_PLAYING) {
            phase = Phase.LUMEN_PLAYING;
            getMainLumen().setTravelLength(new Radical());
            animate(getMainLumen(), null, 0);
        }
    }

    public void animate(Lumen lumen, Line fromHere, long TTR) {

        if(lumen != null && lumen.isActive()) {

            ArrayList<Line> next = path.next(lumen.position, fromHere);

            // Controllo se l'attuale Lume è sul Bulbo
            Bulb bulb = onBulb(lumen);
            if(bulb != null) {
                int lumenOnTheBulb = 0;
                //Conto il Lumen sul Bulbo.
                List<Lumen> list = lums.getRawListCopy();
                List<Lumen> winnerLums = new ArrayList<>();
                    for(Lumen l : list){
                        // Se un Lumen è su un segmento che porta al Bulb AND
                        // la sua distanza percorsa+SuddettoSegmento è uguale alla mia distanza percorsa
                        // Allora sono entrambi sul bulbo.
                        boolean onTheBulb = l.onTheWayOn(bulb.position);
                        boolean travelEql = l.traveledAsMuchAs(lumen) ;

                        if(onTheBulb && travelEql) {
                            lumenOnTheBulb++;
                            winnerLums.add(l);
                        }
                    }

                    if (lumenOnTheBulb == bulb.getNeed()){
                        boolean won = true;
                        boolean otherActiveLumen = false;

                        for(Lumen l : winnerLums) l.fadeOff(false);
                        for(Lumen l : lums) if(l.isActive()) otherActiveLumen = true;
                        for(Bulb b : blbs) if(!b.equals(bulb) && !b.isSaturated()) won = false;

                        if(won) {
                            bulb.notifySaturation(this, true);
                            softStop();
                        } else if (otherActiveLumen){
                            bulb.notifySaturation(this, false);
                        } else {
                            bulb.notifyUnderSaturation();
                            onStopButtonClick();
                        }
                    } else if (lumenOnTheBulb > bulb.getNeed()){
                        bulb.notifyOverSaturation();
                        onStopButtonClick();
                    } else {
                        bulb.notifyUnderSaturation();
                        onStopButtonClick();
                    }


            // Controllo se l'attuale Lume ha finito la sua corsa.
            } else if (next.size() == 0) {
                fadeOff(lumen);

            // Controllo se non ho superato la quantità massima di Lumes.
            } else if ( lums.size() + next.size() - 1 < Consts.lumenMax) {
                Lumen lumenToAnimate;

                //Controllo che esista già un Lumen che, nello stesso momento, faccia questo specifico split.
                for(int i = 0; i < next.size(); i++){
                    boolean commonFateLumenFound = false;
                    Line.Direction futureDirection = next.get(i).suggestDirection(lumen);
                    List<Lumen> list = lums.getRawListCopy();
                    for(Lumen l : list){

                        boolean differentLumen = l != lumen;
                        boolean headingOnMe = l.onTheWayOn(lumen.position);
                        boolean sameDistance = l.traveledAsMuchAs(lumen);
                        boolean differentSegment = !next.get(i).equals(l.laying);

                        if (headingOnMe  && differentLumen && sameDistance && differentSegment) {
                            // Ho trovato un lumen diverso da se stesso con lo stesso fato: quindi non lo faccio partire.
                            commonFateLumenFound = true;
                            break;
                        }
                    }
                    if(!commonFateLumenFound) {

                        lumenToAnimate = lumen.duplicate();
                        lums.add(lumenToAnimate);

                        long duration = ((long) (next.get(i).length().pixelLength() / lumenToAnimate.getSpeed())) - TTR;
                        lumenToAnimate.startAnimation(next.get(i), duration);
                    }
                    lums.remove(lumen);
                }

            // Se arrivo qui significa che ho superato la quantità massima di Lumes.
            } else {
                reset();
            }

        }
    }

    private boolean nearEnough(Dot one, Dot two) {
        // TODO Questo viene usato solo per le stelle adesso: trova una soluzione alternativa!
        // E NON USARLO!
        return Math.abs(one.pixelX()-two.pixelX()) < 8 && Math.abs(one.pixelY()-two.pixelY()) < 8;
    }

    private Bulb onBulb(Lumen lumen) {
        for(Bulb b : blbs){
            if(lumen.position.equals(b.position)) return b;
        }
        return null;
    }

    public void fadeOff(Lumen lumen){
        lums.remove(lumen);
        boolean noLumsLeft = true;
        for(Lumen l : lums) if(l.active) noLumsLeft = false;
        if(noLumsLeft) {
            hud1.setButtonsEnabled(true);
            unpickStars();
            for(Bulb b : blbs.getRawListCopy()) b.renewWhenAnimationFinishes();
        }
        lumen.fadeOff(noLumsLeft);
    }

    protected OnTouchListener getSegmentCreator() {
        return new SegmentCreatorListener(this);
    }

    // Callback dei Bottoni
    public void onStartButtonClick(){
        hud1.setButtonsEnabled(false);
        start();
        updateMinDist();
    }

    public void onResetButtonClick(){
        hud1.setButtonsEnabled(true);
        reset();
        updateMinDist();
    }

    public void onStopButtonClick(){
        hud1.setButtonsEnabled(true);
        hardStop();
    }

    public void onBackButtonClick(){
        toMenu();
    }

    public void onUndoButtonClick(){
        if(path != null) path.undo();
        updateMinDist();
    }

    public void onModeButtonClick() {
        setMode(!showLength);
    }

    public void setMode(boolean b) {
        path.setMode(b);
        showLength = b;
    }


    //Bulb Managment
    public void addBulb(Dot p) {
        if(blbs == null) blbs = new DList<>();
        Bulb bulb = new Bulb(p);
        bulb.startAnimation();
        blbs.add(bulb);
        collect(blbs);
    }

    public void addBulb(Bulb bulb) {
        if(blbs == null) blbs = new DList<>();
        bulb.startAnimation();
        blbs.add(bulb);
        collect(blbs);
    }

    public void removeBulb(Bulb b) {
        if(blbs == null) return;
        blbs.remove(b);
    }

    public void hardStop() {
        satisfiedBulbs = 0;
        softStop();
        unpickStars();
        setMainLumen(spnt);
        for(Bulb bulb : blbs.getRawListCopy()){
            bulb.renewWhenAnimationFinishes();
        }
    }

    public void softStop(){
        while (lums.size() > 0) {
            getMainLumen().fadeOff(false);
            lums.remove(getMainLumen());
        }
        updateMinDist();
    }

    public void reset() {

        if(phase == Phase.LUMEN_PLAYING) { MessagesHelper.notifyTooManyLumens(getContext());}
        hud1.setButtonsEnabled(true);
        for(Bulb b: blbs.getRawListCopy()) b.notifyReset();
        path.reset();
        hardStop();

    }

    private void unpickStars() {
        for(Star s : strs){
            s.setPicked(false);
        }
    }


    public void setTouchEnabled(boolean touchEnabled) {
        this.touchEnabled = touchEnabled;
    }

    public boolean isTouchEnabled() {
        return touchEnabled;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        //if it is the first time the thread starts
        if(thread.getState() == Thread.State.NEW){
            MainThread.setRunning(true);
            thread.start();
        }

        //after a pause it starts the thread again
        else
        if (thread.getState() == Thread.State.TERMINATED){
            thread = new MainThread(getHolder(), this);
            MainThread.setRunning(true);
            thread.start(); // Start a new thread
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
                // try again shutting down the thread
            }
        }
    }


    public void update(){
        for(SchemeLayoutDrawable drawable : drawableCollection) {
            drawable.update();
        }

        // Aggiornamento di fase;
        if(phase == Phase.COMPLETING_TRANSITION && ends.hasFadedIn()){
            phase = Phase.RESULT;
        }

    }

    public void render(Canvas canvas){
        if(canvas != null && drawElements) {

            clear(canvas);
            render(canvas, bckg);
            render(canvas, grid);
            render(canvas, path);
            render(canvas, obst);
            render(canvas, strs);
            render(canvas, blbs);
            render(canvas, lums);
            render(canvas, hud1);
            render(canvas, tost);

            if(phase.isPhaseOfResult()){
                render(canvas, hud2);
                render(canvas, ends);
            }

        }
    }

    private void render(Canvas canvas, SchemeLayoutDrawable drawable){
        if(drawable != null) drawable.render(canvas);
    }

    protected void clear(Canvas canvas) {
        canvas.drawRect(0,0,canvas.getWidth(), canvas.getHeight(), bckg.getBackPaint());
    }

    //Utilità:

    private long bgnTime = -1;
    public long getTimeElapsed(){
        return System.currentTimeMillis() - bgnTime;
    }
    public void setTimeElapsed(long ms){ bgnTime = System.currentTimeMillis() - ms; }

    private float valueOfNow(float bgnPoint, float endPoint, long bgnTime, long endTime, AnimType type) {
        return Utils.valueOfNow((float) getTimeElapsed(), bgnPoint, endPoint, bgnTime, endTime,type);
    }

    protected float valueOfNow(float bgnPoint, float endPoint, long bgnTime, long endTime) {
        return valueOfNow(bgnPoint, endPoint, bgnTime, endTime, AnimType.DEFAULT);
    }

    public void startActivity(Class<?> cls){
        startActivity(new Intent(getContext(), cls));
    }

    public void startActivity(Intent intent){
        MainThread.setRunning(false);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        getContext().startActivity(intent);
    }

    public void toMenu() {
        Intent i = new Intent(getContext(), MenuActivity.class);
        i.putExtra(getContext().getString(R.string.schemeToLoad), getCode());
        startActivity(i);
    }

    public void toTitle() {
        Intent i = new Intent(getContext(), TitleActivity.class);
        i.putExtra(getContext().getString(R.string.schemeToLoad), getCode());
        startActivity(i);
    }

    public DList<Bulb> getBulbs() {
        return blbs;
    }

    void incrementNeedOfAll() {
        for(Bulb bulb : blbs.getRawListCopy()) {
            int need = bulb.getNeed() + 1;
            if (need > MAX_NEED) {
                bulb.setNeed(1);
            } else {
                bulb.setNeed(need);
            }
        }
    }


    public void saturationNotified() {
        SaveFileManager.writeScheme(getContext(), new SchemeResult(code, starsPicked(), 100, true));

        phase = Phase.COMPLETING_TRANSITION;
        ends.setStarsValue(starsPicked(), strs.size() + getBulbs().size());

        notifyFadeOutToEverything();
        hud2.notifyFadeIn();
        ends.notifyFadeIn();
        path.notifyTextFadeOut();
    }

    private void notifyFadeOutToEverything() {
        grid.notifyFadeOut();
        lums.notifyFadeOut();
        hud1.notifyFadeOut();
        if(tost != null) tost.notifyFadeOut();
    }

    private int starsPicked() {
        int r = getBulbs().size();
        for(Star star: strs) if (star.isPicked()) r++;
        return r;
    }

    public void setCode(String code) {
        this.code = code;
        if(hud1 != null) hud1.updateVisibility(this);
    }

    public String getCode() {
        return code;
    }

    public String getSector() {
        return "PlaceHolderSector";
    }

    //TODO: Questa è da cambiare del tutto. Pensare a dove metterla
    public void updateMinDist(){
        if(blbs != null)
         minDist = path.minDistance(spnt, blbs.get(0).position);
    }

    public Radical getMinLength() {

        return minDist;
    }

    public boolean illegalPosition(Segment segment) {
        return segment.intersecatesAnyOf(getObstructors());
    }


    public void addStars(List<Dot> strs) {
        for(Dot d : strs){
            addStar(d);
        }
    }

    public void addStar(Dot d) {
        Star star = new Star(d);
        star.startAnimation();
        this.strs.add(star);
    }

    public void removeStar(Star s) {
        uncollect(s);
        strs.remove(s);
    }

    public void removeObstacle(Obstacle o) {
        uncollect(o);
        obst.remove(o);
    }


    public void setToast() {
        String s = Consts.getString(getContext(), "LevelString" + getCode());
        if(Utils.isNullOrEmpty(s)){
            tost = null;
        }
            else {
            tost = new SchemeToast(s);
            collect(tost);
        }
    }

    @Override
    public void onUpdate(SchemeLayoutDrawable sender) {
        if(strs != null && sender.position != null)
        for(Star s : strs){
            if(!s.isPicked() && s.gamma != null && nearEnough(s.gamma, sender.position))
            {
                s.pick();
            }
        }
    }

    @Override
    public void onButtonClick(Button button) {
        Button.Action action = button.getAction();
        if(phase.isPhaseOfPlaying()){
            switch (action){
                case START: onStartButtonClick(); break;
                case RESET: onResetButtonClick(); break;
                case STOP:  onStopButtonClick();  break;
                case UNDO:  onUndoButtonClick();  break;
                case BACK:  onBackButtonClick();  break;
                case MODE:  onModeButtonClick();  break;
            }
        } else if (phase.isPhaseOfResult()){
            switch (action){
                case NEXT: onNextButtonClick(); break;
                case MENU: onMenuButtonClick(); break;
                case REDO: onRedoButtonClick();  break;
            }
        }
    }

    private void onRedoButtonClick() {
        MainThread.setRunning(false);
        MainActivity.play(getActivity(), code);
    }

    private void onMenuButtonClick() {
        toMenu();
    }

    public void onNextButtonClick() {
        MainThread.setRunning(false);
        String code = nextLevelCode();
        if(Utils.isNullOrEmpty(code)){
            if(Consts.DEMO){
                toTitle();
            } else {
                toMenu();
            }
        } else {
            MainActivity.play(getActivity(), code);
        }
    }

    private String nextLevelCode() {
        try {
            String newCode = String.format("%03d", Integer.parseInt(getCode()) + 1);
            if(Consts.getSchemeLayout(newCode, new SchemeLayout(getContext())) != null) return newCode;
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Activity getActivity() {
        return (Activity) getContext();
    }

    public boolean hasSecondaryButtons() {
        String[] onlyPrimaryLevels = {"000", "001", "002", "003"};
        return !Arrays.asList(onlyPrimaryLevels).contains(code);
    }

    public Grid getGrid() {
        return grid;
    }

    public Path getPath() {
        return path;
    }

    public Phase getPhase() {
        return phase;
    }

    public void setBulbs(List<Pair<Dot, Integer>> bulbs) {
        for (Pair<Dot, Integer> pair : bulbs) {
            Bulb b = new Bulb(pair.first);
            b.setNeed(pair.second);
            addBulb(b);
        }
    }

    public boolean isShowLength() {
        return showLength;
    }
}