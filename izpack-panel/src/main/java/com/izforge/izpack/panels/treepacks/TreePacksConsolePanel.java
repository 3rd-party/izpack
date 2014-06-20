/*
 * IzPack - Copyright 2001-2008 Julien Ponge, All Rights Reserved.
 * 
 * http://izpack.org/
 * http://izpack.codehaus.org/
 * 
 * Copyright 2003 Jonathan Halliday
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.izforge.izpack.panels.treepacks;

import java.util.List;
import java.util.Properties;

import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.data.Pack;
import com.izforge.izpack.api.exception.ResourceNotFoundException;
import com.izforge.izpack.api.handler.Prompt;
import com.izforge.izpack.api.handler.Prompt.Type;
import com.izforge.izpack.api.resource.Messages;
import com.izforge.izpack.installer.console.AbstractConsolePanel;
import com.izforge.izpack.installer.console.ConsolePanel;
import com.izforge.izpack.installer.panel.PanelView;
import com.izforge.izpack.installer.util.PackHelper;
import com.izforge.izpack.panels.packs.PacksModel;
import com.izforge.izpack.util.Console;

/**
 * Console implementation for the TreePacksPanel.
 * <p/>
 * Based on PacksConsolePanelHelper
 *
 * @author Sergiy Shyrkov
 * @author Dustin Kut Moy Cheung
 */
public class TreePacksConsolePanel extends AbstractConsolePanel implements ConsolePanel
{
    private Messages messages;
    private final Prompt prompt;

    private PacksModel packsModel;
    private static final String LANG_FILE_NAME = "packsLang.xml";


    private static final String REQUIRED = "TreePacksPanel.required";
    private static final String DEPENDENT = "TreePacksPanel.dependent";
    private static final String DONE = "TreePacksPanel.done";

    private static final String CONFIRM = "TreePacksPanel.confirm";
    private static final String NUMBER = "TreePacksPanel.no.number";
    private static final String PROMPT = "TreePacksPanel.prompt";
    private static final String INVALID = "TreePacksPanel.invalid";
    private static final String REQUIRED_SPACE = "TreePacksPanel.space.required";
    
    /**
     * Constructs a {@code TreePacksConsolePanel}.
     *
     * @param panel  the parent panel/view. May be {@code null}
     * @param prompt the prompt
     */
    public TreePacksConsolePanel(PanelView<ConsolePanel> panel, Prompt prompt)
    {
        super(panel);
        this.prompt = prompt;
    }

    /**
     * Runs the panel using the supplied properties.
     *
     * @param installData the installation data
     * @param properties  the properties
     * @return <tt>true</tt> if the installation is successful, otherwise <tt>false</tt>
     */
    public boolean run(InstallData installData, Properties properties)
    {
        return true;
    }

    /**
     * Runs the panel using the specified console.
     *
     * @param installData the installation data
     * @param console     the console
     * @return <tt>true</tt> if the panel ran successfully, otherwise <tt>false</tt>
     */
    public boolean run(InstallData installData, Console console)
    {
        List<Pack> selectedPacks;
        packsModel = new PacksModel(installData);

        try
        {
            messages = installData.getMessages().newMessages(LANG_FILE_NAME);
        }
        catch (ResourceNotFoundException exception)
        {
            // no packs messages resource, so fall back to the default
            messages = installData.getMessages();
        }

        displayPackMenu(installData);
        out(Type.INFORMATION, installData.getMessages().get(DONE));
        selectedPacks = packsModel.updatePacksToInstall();

        if (selectedPacks.size() == 0)
        {
            out(Type.WARNING, "You have not selected any packs!");
            out(Type.WARNING, "Are you sure you want to continue?");
        }

        return promptEndPanel(installData, console);
    }

    private void out(Type type, String message)
    {
        prompt.message(type, message);
    }

    /**
     * Helper method to ask/check if the pack can/needs to be installed
     * If top-level pack, square brackets will be placed in between
     * the pack id.
     *
     * It asks the user if it wants to install the pack if:
     * 1. the pack is not required
     * 2. the pack has no condition string
     *
     * @param installData       - Database of izpack
     */
    private void displayPackMenu(final InstallData installData)
    {
        java.io.Console console = System.console();

        printPackMenu();
        List<Pack> visiblePacks = packsModel.getVisiblePacks();
        int maxRow = visiblePacks.size();
        while (true)
        {
            int choice = -1;
            try
            {
                choice = (Integer.parseInt(console.readLine())) -1;
            }
            catch(NumberFormatException e)
            {
                out(Type.WARNING, installData.getMessages().get(NUMBER));
                continue;
            }

            if (choice <= maxRow && choice >= 0)
            {
                if (!packsModel.isCheckBoxSelectable(choice))
                {
                    out(Type.WARNING, installData.getMessages().get(INVALID));
                }
                else
                {
                    packsModel.toggleValueAt(choice);
                    printPackMenu();
                }
            }
            else if (choice == -1)
            {
                break;
            }
            else
            {
                out(Type.WARNING, installData.getMessages().get(INVALID));
            }
        }
    }

    public void printPackMenu()
    {
        int row = 0;
        int totalSize = packsModel.getTotalByteSize();

        for (Pack pack : packsModel.getVisiblePacks())
        {
            if (pack.isRequired())
            {
                System.out.println(generateRowEntry(row, pack, REQUIRED));
            }
            else if(pack.hasDependencies())
            {
                System.out.println(generateRowEntry(row, pack, DEPENDENT));
            }
            else
            {
                System.out.println(generateRowEntry(row, pack, ""));
            }
            row++;
        }

        System.out.println(messages.get(REQUIRED_SPACE) + " " + Pack.toByteUnitsString(totalSize));
        System.out.println(messages.get(CONFIRM));
        System.out.println(messages.get(PROMPT));
    }

    /**
     *
     * @param row row to be displayed (starting from 0)
     * @param pack the associated pack to display
     * @param extra any extra information to be displayed for the pack
     * @return String to display a row of the packs selection menu
     */
    private String generateRowEntry(int row, Pack pack, String extra)
    {
        return String.format("%-4d [%s] %-15s [%s] (%-4s)",
                row+1,
                (packsModel.isChecked(row) ? "x" : " "),
                messages.get(extra),
                PackHelper.getPackName(pack, messages),
                pack.toByteUnitsString(pack.getSize()));
    }
}
