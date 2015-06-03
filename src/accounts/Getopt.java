package accounts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public final class Getopt
{
    public static final HashMap<String, String> ALL_OPTS      = new HashMap<String, String>();
    public static final String                  ERROR         = "ERROR";
    /**
     * This functions provides the similar features as "C" language getopt
     * interface. It returns a HashTable. In case of error it adds an entry for
     * key "ERROR" in the hashtable.
     **/
    public static final String                  CONTRNT_S     = "S";
    public static final String                  CONTRNT_I     = "I";
    public static final String                  CONTRNT_F     = "F";
    public static final String                  CONTRNT_NOARG = "";
    static
    {
        ALL_OPTS.put("A", Getopt.CONTRNT_S);
        ALL_OPTS.put("file", Getopt.CONTRNT_S);
        ALL_OPTS.put("test", Getopt.CONTRNT_S);
        ALL_OPTS.put("test1", Getopt.CONTRNT_S);
    }

    private Getopt()
    {
        // Utility class should not be instantiated
    }

    public static HashMap<String, String> processCommandLineArgL(final String[] aArgs, final Map<String, String> aConstraints)
    {
        return processCommandLineArgL(aArgs, aConstraints, false);
    }

    public static HashMap<String, String> processCommandLineArgL(final String[] aArgs, final Map<String, String> aConstraints,
                                                                 final boolean aSubstring)
    {
        return processCommandLineArgL(aArgs, aConstraints, false, null);
    }

    public static HashMap<String, String> processCommandLineArgL(final String[] aArgs, final Map<String, String> aConstraints,
                                                                 final boolean aSubstring, final ArrayList<String> aNoSwitchArgs)
    {
        final Map<String, String> newConstraints = new HashMap<String, String>();
        final Map<String, String> deleteList = new HashMap<String, String>();
        if (aSubstring)
        {
            for (final Iterator<String> it = aConstraints.keySet().iterator(); it.hasNext();)
            {
                final String key = it.next();
                for (int i = 1; i <= key.length(); i++)
                {
                    final String subkey = key.substring(0, i);
                    if (subkey.equals(key))
                    {
                        newConstraints.put(subkey, key);
                    } else
                    {
                        if (newConstraints.containsKey(subkey))
                        {
                            final String previousMappedKey = newConstraints.get(subkey);
                            if (!previousMappedKey.equals(subkey))
                            {
                                deleteList.put(subkey, "");
                            }
                        } else
                        {
                            newConstraints.put(subkey, key);
                        }
                    }
                }
            }
            for (final Iterator<String> it = deleteList.keySet().iterator(); it.hasNext();)
            {
                newConstraints.remove(it.next());
            }
        } else
        {
            for (final Iterator<String> it = aConstraints.keySet().iterator(); it.hasNext();)
            {
                final String key = it.next();
                newConstraints.put(key, key);
            }
        }
        final HashMap<String, String> argHash = new HashMap<String, String>();
        String key = null;
        for (int i = 0; i < aArgs.length; i++)
        {
            if (aArgs[i].startsWith("-"))
            {
                if (aNoSwitchArgs != null && aNoSwitchArgs.size() > 0)
                {
                    argHash.put(ERROR, "Option " + aArgs[i] + " should be specfied before " + aNoSwitchArgs.get(0));
                    return argHash;
                }
                final String optName = aArgs[i].substring(1);
                if (key != null)
                {
                    argHash.put(key, "");
                }
                key = optName;
                if (newConstraints.containsKey(key))
                {
                    key = newConstraints.get(key); // Replace the abbr with
                                                   // full string
                    final String contraintType = aConstraints.get(key);
                    if (contraintType.length() <= 0)
                    {
                        if (argHash.get(key) != null)
                        {
                            argHash.put(ERROR, "Option -" + key + " specified more than once in command line.");
                            return argHash;
                        }
                        argHash.put(key, "");
                        key = null;
                    }
                } else
                {
                    argHash.put(ERROR, "Unrecognized option: -" + key);
                    return argHash;
                }
            } else
            {
                if (key == null)
                {
                    if (aNoSwitchArgs != null)
                    {
                        // Trailing values with no command line switches.
                        aNoSwitchArgs.add(aArgs[i]);
                    } else
                    {
                        argHash.put(ERROR, "Option " + aArgs[i] + " does not start with -");
                        return argHash;
                    }
                } else
                {
                    if (argHash.get(key) != null)
                    {
                        argHash.put(ERROR, "Option -" + key + " specified more than once in command line.");
                        return argHash;
                    }
                    argHash.put(key, aArgs[i]);
                    key = null;
                }
            }
        }
        if (key != null)
        {
            argHash.put(key, "");
        }
        if (argHash.get("?") != null)
        {
            argHash.put(ERROR, "");
            return argHash;
        }
        if (argHash.get("help") != null)
        {
            argHash.put(ERROR, "");
            return argHash;
        }
        // Check for unrecognized option.
        for (final Entry<String, String> entry : argHash.entrySet())
        {
            key = entry.getKey();
            final String value = entry.getValue();
            if (!newConstraints.containsKey(key))
            {
                argHash.put(ERROR, "Unrecognized option: -" + key);
                return argHash;
            }
            final String origkey = newConstraints.get(key);
            final String contraintType = aConstraints.get(origkey);
            if (contraintType.length() > 0)
            {
                if ("".equals(value))
                {
                    argHash.put(ERROR, "Option -" + key + " requires an argument");
                    return argHash;
                }
            } else
            {
                if (!"".equals(value))
                {
                    argHash.put(ERROR, "Option -" + key + " should not have an argument");
                    return argHash;
                }
            }
            if (CONTRNT_I.equalsIgnoreCase(contraintType))
            {
                try
                {
                    new Integer(value);
                } catch (final Exception ex)
                {
                    argHash.put(ERROR, "Option -" + key + " requires an integer argument");
                    return argHash;
                }
            } else if (CONTRNT_F.equalsIgnoreCase(contraintType))
            {
                try
                {
                    new Float(value);
                } catch (final Exception ex)
                {
                    argHash.put(ERROR, "Option -" + key + " requires an floating point number as argument");
                    return argHash;
                }
            }
        }
        return argHash;
    }

    public static HashMap<String, String> processCommandLineArg(final String[] aArgs, String aConstraints)
    {
        final HashMap<String, String> argHash = new HashMap<String, String>();
        int i = 0;
        while (i < aArgs.length)
        {
            if (aArgs[i].startsWith("-"))
            {
                // 3 possibilities
                // -Hhello
                // -H hello
                // -H
                if (aArgs[i].length() > 2)
                {
                    argHash.put(aArgs[i].substring(1, 2), aArgs[i].substring(2));
                } else
                {
                    if (i + 1 == aArgs.length || aArgs[i + 1].startsWith("-"))
                    {
                        argHash.put(aArgs[i].substring(1), "");
                    } else
                    {
                        argHash.put(aArgs[i].substring(1), aArgs[i + 1]);
                        i++;
                    }
                }
            } else
            {
                argHash.put(ERROR, "Option " + aArgs[i] + "does not start with -");
                return argHash;
            }
            i++;
        }
        if (argHash.get("?") != null)
        {
            argHash.put(ERROR, "");
        }
        if (argHash.get(ERROR) == null && aConstraints != null)
        {
            // to avoid StringIndexOutOfBoundsException below append a dummy
            // trailing character.
            aConstraints += "?";
            // Check for unrecognized option.
            for (final Entry<String, String> entry : argHash.entrySet())
            {
                final String key = entry.getKey();
                final int index = aConstraints.indexOf(key);
                if (index == -1)
                {
                    argHash.put(ERROR, "Unrecognized option: -" + key);
                    return argHash;
                } else
                {
                    if ("".equals(entry.getValue()) && aConstraints.charAt(index + 1) == ':')
                    {
                        argHash.put(ERROR, "Option -" + key + " requires an argument");
                        return argHash;
                    }
                }
            }
        }
        return argHash;
    }

    public static void usage(final String err)
    {
        if (err != null && !"".equals(err))
        {
            System.out.println(err);
        }
        System.out.println("Usage: -A action options\n");
        System.exit(1);
    }

    public static void main(final String[] args)
    {
        final Map<String, String> argHash = processCommandLineArgL(args, ALL_OPTS, true);
        if (argHash.get(Getopt.ERROR) != null)
        {
            usage(argHash.get(Getopt.ERROR));
        }
        System.out.println("option A=" + argHash.get("A"));
        System.out.println("option file=" + argHash.get("file"));
        System.out.println("option test=" + argHash.get("test"));
        System.out.println("option test1=" + argHash.get("test1"));
    }
}
