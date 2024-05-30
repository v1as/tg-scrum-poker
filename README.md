# TG Scrum Poker Bot

## Overview
The TG Scrum Poker Bot is a Telegram bot designed to facilitate planning poker sessions for Agile teams. It helps teams to estimate tasks collaboratively by starting, repeating, and managing voting sessions for various tasks.

## Try It Out
You can try the bot by visiting: [FoScrumPokerBot](https://t.me/FoScrumPokerBot)

## Getting Started
To start using the bot, add it to chat and enter the command:
```
/start TGSM-123 Clean some oranges
```

## Available Commands

- **/start** 
  - Usage: `/start [TASK_ID] [TASK_DESCRIPTION]` 
  - Description: Begins a voting session for a specified task. If no task is specified, it will start voting on a task from the list added via `/add_all`.
  - Example: `/start http://your.jira.ru/TGSM-123 Clean some oranges`

- **/repeat**
  - Description: Repeats the voting session for the last closed task.
  - Usage: Simply enter `/repeat`

- **/add_all**
  - Usage: `/add_all [TASK_LIST]`
  - Description: Adds a list of tasks to the queue for future voting sessions.
  - Example: 
    > /add_all http://your.jira.ru/TGSM-124 Wash some apples   
    http://your.jira.ru/TGSM-125 Slice some pineapples  
    http://your.jira.ru/TGSM-125 Buy some carrot
  - Note: Each task after first should start on new line 

- **/clear**
  - Description: Clears the current queue of tasks.
  - Usage: Simply enter `/clear`