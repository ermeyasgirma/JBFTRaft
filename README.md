# JRaft

## Overview
Raft is a consensus algorithm designed to manage a replicated log, ensuring that a cluster of computers can maintain a consistent state even in the presence of failures. It was developed to be more understandable compared to other consensus algorithms like Paxos.

## Getting Started

  ### Prerequisites

    1. java 8 or higher

  ### Clone the repository

    $ git clone https://github.com/ermeyasgirma/JRaft.git

    $ cd jraft

  ###  Build the project 

    $ javac main/*.java

    $ javac messages/*.java

  ### Run the program

    Open as many terminals as you would like there to be nodes in your raft algorithm instance, say n nodes

    Choose n different ports, say m, m+1, ... , m + n - 1

    In each terminal run the following 

      $ java main/JRaft m m+1 ... m + n - 1

          Note: in each terminal a different port should be the first argument (the port for that node), the order of the other nodes does not matter

      Interacting with the servers

          1. To send a message to the node to be stored just enter any non-empty string into the terminal

              $ hi

          2. To check if the message has been replicated in other nodes enter "getlog"

              $ getlog

          3. To kill all nodes simply enter "exit" into the terminal

              $ exit

## Key Concepts
  1. Cluster of Nodes: Raft manages a cluster of nodes (servers). Each node can be in one of three states: Leader, Follower, or Candidate.

  2. Leader Election: One node is elected as the leader. The leader handles all client interactions and log management. The other nodes (followers) replicate the leader’s log       
     entries.

  3. Log Replication: The leader appends commands to its log and then replicates these entries to the follower nodes. Once a majority of nodes have appended the entry, the leader        commits the entry to its state machine.

  4. Consistency and Fault Tolerance: Raft ensures that the log is consistent across the cluster. If the leader crashes, a new leader is elected from the followers. The new leader       continues log replication, ensuring that all nodes eventually reach the same state.

## Raft Algorithm Phases
  1. Leader Election:

     - Each node starts as a follower.

     - If a follower does not receive communication from a leader for a certain period, it transitions to a candidate state and initiates an election.

     - The candidate requests votes from other nodes. A node votes for the first candidate it hears from.

     - If a candidate receives votes from a majority of the nodes, it becomes the leader.

     - The leader then starts sending heartbeat messages to assert its leadership.

  2. Log Replication:

     - The leader receives commands from clients and appends them to its log.

     - The leader sends the new log entries to followers.

     - Followers append the entries to their logs and send acknowledgments back to the leader.

     - Once the leader receives acknowledgments from a majority of followers, it commits the entries and applies them to its state machine.

     - The leader then notifies the followers about the committed entries.

  3. Safety:

     - Raft guarantees that log entries are applied in the same order on all nodes.

     - An entry is considered committed once it is safely stored on a majority of the nodes.

     - Raft handles network partitions and ensures that a single, consistent leader is elected.

## Why Raft? 

  Understandability: Raft is designed to be easier to understand compared to other consensus algorithms.

  Strong Consistency: Ensures that all nodes in the cluster agree on the same state.

  Fault Tolerance: Can tolerate the failure of up to half of the nodes in the cluster.


## Acknowledgements

  - The original Raft paper by Diego Ongaro and John Ousterhout

  - raft.github.io for providing detailed resources on Raft

  - https://www.youtube.com/watch?v=uXEYuDwm7e4&t=1665s (easy to understand youtube video explaining the algorithm)
