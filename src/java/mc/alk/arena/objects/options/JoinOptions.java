package mc.alk.arena.objects.options;

import mc.alk.arena.BattleArena;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.arenas.Arena;
import mc.alk.arena.objects.exceptions.InvalidOptionException;
import mc.alk.arena.objects.spawns.SpawnLocation;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.util.MessageUtil;
import mc.alk.arena.util.MinMax;
import mc.alk.arena.util.PermissionsUtil;
import mc.alk.arena.util.TeamUtil;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JoinOptions {


    public static enum JoinOption{
        ARENA("<arena>",false), TEAM("<team>",false),
        WANTEDTEAMSIZE("<teamSize>",false);

        final public boolean needsValue;
        final String name;
        JoinOption(String name, boolean needsValue){
            this.needsValue = needsValue;
            this.name = name;
        }
        public String getName(){
            return name;
        }
        public static JoinOption fromName(String str){
            str = str.toUpperCase();
            try {return JoinOption.valueOf(str);} catch (Exception e){/*do nothing*/}
            throw new IllegalArgumentException();
        }
        public static String getValidList() {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (JoinOption r: JoinOption.values()){
                if (!first) sb.append(", ");
                first = false;
                String val = "";
                switch (r){
                    default: break;
                }
                sb.append(r.getName()).append(val);
            }
            return sb.toString();
        }
    }

    /** All options for joining */
    final Map<JoinOption,Object> options = new EnumMap<JoinOption,Object>(JoinOption.class);

    /** Location they have joined from */
    Location joinedLocation = null;

    MatchParams params;

    MinMax teamSize;

    /** When the player joined, (defaults to when the JoinOptions was created) */
    long joinTime;

    public JoinOptions(){
        joinTime = System.currentTimeMillis();
    }

//    public boolean matches(Arena arena) {
//        if (options.containsKey(JoinOption.ARENA)){
//            Arena a = (Arena) options.get(JoinOption.ARENA);
//            return a == null || arena.matches(a);
//        }
//        return true;
//    }
//
//    public Collection<String> getInvalidMatchReasons(Arena arena) {
//        List<String> reasons = new ArrayList<String>();
//        if (options.containsKey(JoinOption.ARENA)) {
//            Arena a = (Arena) options.get(JoinOption.ARENA);
//            if (a == null) {
//                reasons.add(" arena " + arena + " is null");
//            } else if (!arena.matches(a)) {
//                reasons.addAll(arena.getInvalidMatchReasons(a));
//            }
//        }
//        return reasons;
//    }

//    public boolean matches(MatchParams params) {
////		return ArenaSize.matchesNTeams(nTeams, params.getNTeams());
//        return ArenaSize.lower(teamSize, params.getTeamSize());
//    }
//
//    public static boolean matches(JoinOptions jo, Match match) {
//        return jo==null || (jo.matches(match.getArena()) && jo.matches(match.getParams()));
//    }

    public void setJoinLocation(Location joinLocation) {
        this.joinedLocation = joinLocation;
    }

    public boolean nearby(Arena arena, double distance) {
        if (joinedLocation == null)
            return false;
        UUID wid = joinedLocation.getWorld().getUID();
        Location arenajoinloc = arena.getJoinLocation();
        if (arenajoinloc != null){
            return (wid == arenajoinloc.getWorld().getUID() &&
                    arenajoinloc.distance(joinedLocation) <= distance);
        }

        for (List<SpawnLocation> list : arena.getSpawns()){
            for (SpawnLocation l : list){
                if (l.getLocation().getWorld().getUID() != wid)
                    return false;
                if (l.getLocation().distance(joinedLocation) <= distance)
                    return true;

            }
        }
        return false;
    }

    public boolean sameWorld(Arena arena) {
        UUID wid = joinedLocation.getWorld().getUID();
        Location arenajoinloc = arena.getJoinLocation();
        if (arenajoinloc != null){
            return (wid == arenajoinloc.getWorld().getUID());}

        for (List<SpawnLocation> list : arena.getSpawns()){
            for (SpawnLocation l : list){
                if (l.getLocation().getWorld().getUID() != wid)
                    return false;
            }
        }
        return true;
    }

    public static JoinOptions parseOptions(final MatchParams omp, ArenaTeam t, ArenaPlayer player, String[] args)
            throws InvalidOptionException, NumberFormatException{
        JoinOptions jos = new JoinOptions();
        MatchParams mp = new MatchParams(omp.getType());
        mp.setParent(omp);
        jos.setMatchParams(mp);
        jos.setJoinTime(System.currentTimeMillis());
        jos.joinedLocation = player.getLocation();
        Map<JoinOption,Object> ops = jos.options;
        Arena arena = null;
        String lastArg = args.length > 0 ? args[args.length-1] : "";
        int length = args.length;

        for (int i=0;i<length;i++){
            String op = args[i];
            if (op.isEmpty())
                continue;
            Object obj = null;
            op = MessageUtil.decolorChat(op);
            Arena a = BattleArena.getBAController().getArena(op);
            if (a != null){
                if (arena != null){
                    throw new InvalidOptionException("&cYou specified 2 arenas!");}
                if (!a.valid()){
                    throw new InvalidOptionException("&cThe specified arena is not valid!");}
                arena = a;
                ops.put(JoinOption.ARENA, arena);
                continue;
            }
            try {
                Integer wantedSize = Integer.valueOf(op);
                ops.put(JoinOption.WANTEDTEAMSIZE, wantedSize);
                mp.setTeamSize(wantedSize);
                continue;
            } catch (Exception e){
                /* do nothing*/
            }
            Integer teamIndex = TeamUtil.getFromHumanTeamIndex(op);
            if (teamIndex != null){
                if (!PermissionsUtil.hasTeamPerm(player, mp,teamIndex)){
                    throw new InvalidOptionException("&cYou don't have permissions to add this team");}
                ops.put(JoinOption.TEAM, teamIndex);
                continue;
            }

            JoinOption jo;
            try{
                jo = JoinOption.fromName(op);
                if (jo.needsValue && i+1 >= args.length){
                    throw new InvalidOptionException("&cThe option " + jo.name()+" needs a value!");}
            } catch(IllegalArgumentException e){
                throw new InvalidOptionException("&cThe arena or option " + op+" does not exist, \n&cvalid options=&6"+
                        JoinOption.getValidList());
            }
            switch(jo){
                default:
                    break;
            }

            if (!jo.needsValue){
                ops.put(jo,null);
                continue;
            }
            String val = args[++i];
            switch(jo){
                case ARENA:
                    obj = BattleArena.getBAController().getArena(val);
                    if (obj==null){
                        throw new InvalidOptionException("&cCouldnt find the arena &6" +val);}
                    a = (Arena) obj;
                    if (!a.valid()){
                        throw new InvalidOptionException("&cThe specified arena is not valid!");}
                    arena = a;
                default:
                    break;
            }
            ops.put(jo, obj);
        }
        if (arena != null){
            if ((!arena.matches(jos))){
                throw new InvalidOptionException("&cThe arena &6" +arena.getName() +
                        "&c doesn't match your add requirements. "  +
                        StringUtils.join( arena.getInvalidMatchReasons(mp, jos), '\n'));
            }
            mp.setParent(arena.getParams());
        }
        MinMax mm = null;
        try{mm = MinMax.valueOf(lastArg);} catch (Exception e){/* do nothing */}

        if (mm != null)
            ops.put(JoinOption.WANTEDTEAMSIZE, mm);

        jos.params= mp;
        return jos;
    }

    public void setJoinTime(Long currentTimeMillis) {
        this.joinTime = currentTimeMillis;
    }

    public Long getJoinTime(){
        return joinTime;
    }

    public String optionsString(MatchParams mp) {
        StringBuilder sb = new StringBuilder(mp.toPrettyString()+" ");
        for (JoinOption op: options.keySet()){
            sb.append(op.getName());
            if (op.needsValue){
                sb.append("=").append(options.get(op));
            }
            sb.append(" ");
        }
        return sb.toString();
    }

//	public boolean hasTeamSize(){
//		if (options.containsKey(JoinOption.WANTEDTEAMSIZE)){
//			return ((WantedTeamSizePair)options.get(JoinOption.WANTEDTEAMSIZE)).manuallySet;}
//		return false;
//	}
//
//	public Integer getTeamSize(){
//		return ((WantedTeamSizePair)options.get(JoinOption.WANTEDTEAMSIZE)).size;
//	}

    public boolean hasWantedTeamSize() {
        return options.containsKey(JoinOption.WANTEDTEAMSIZE);
    }

    public boolean hasOption(JoinOption option) {
        return options.containsKey(option);
    }

    public Object getOption(JoinOption option) {
        return options.get(option);
    }

    public Object setOption(JoinOption option, Object value) {
        return options.put(option, value);
    }

    public boolean hasArena() {
        return options.containsKey(JoinOption.ARENA) && options.get(JoinOption.ARENA)!=null;
    }

    public Arena getArena() {
        return hasArena() ? (Arena) options.get(JoinOption.ARENA) : null;
    }


    public void setArena(Arena arena) {
        options.put(JoinOption.ARENA, arena);
    }

    public MatchParams getMatchParams() {
        return params;
    }

    public void setMatchParams(MatchParams matchParams) {
        this.params = matchParams;
    }
}
