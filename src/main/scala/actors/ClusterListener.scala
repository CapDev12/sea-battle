package actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.ClusterEvent.{MemberEvent, ReachabilityEvent}
import akka.cluster.typed.{Cluster, Subscribe}

object ClusterListener {

  sealed trait Event

  private final case class MemberChange(event: MemberEvent) extends Event

  private final case class ReachabilityChange(reachabilityEvent: ReachabilityEvent) extends Event

  def apply(): Behavior[Event] = Behaviors.setup { ctx =>

    val memberEventAdapter: ActorRef[MemberEvent] = ctx.messageAdapter(MemberChange)
    Cluster(ctx.system).subscriptions ! Subscribe(memberEventAdapter, classOf[MemberEvent])

    val reachabilityAdapter = ctx.messageAdapter(ReachabilityChange)
    Cluster(ctx.system).subscriptions ! Subscribe(reachabilityAdapter, classOf[ReachabilityEvent])

    Behaviors.receiveMessage { message =>
      message match {
        //        case ReachabilityChange(UnreachableMember(member)) =>
        //          ctx.log.info(s"Member detected as unreachable: $member")
        //        case ReachabilityChange(ReachableMember(member)) =>
        //          ctx.log.info(s"Member back to reachable: $member")
        //
        //        case MemberChange(MemberUp(member)) =>
        //          ctx.log.info(s"Member is Up: ${member.address}")
        //        case MemberChange(MemberJoined(member)) =>
        //          ctx.log.info(s"Member is Joined: ${member.address}")
        //        case MemberChange(MemberRemoved(member, previousStatus)) =>
        //          ctx.log.info(s"Member is Removed: ${member.address} after $previousStatus")

        case msg =>
          ctx.log.info(s"Cluster message: $msg")
      }
      Behaviors.same
    }
  }


}
