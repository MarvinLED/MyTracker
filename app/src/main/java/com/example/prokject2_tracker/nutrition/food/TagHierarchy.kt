package com.example.prokject2_tracker.nutrition.food

/**
 * Traversal over the "implies" graph built from [TagImplication] rows — a child implies its parents,
 * so "vegan" points up at "vegetarisch".
 *
 * Every walk carries a `visited` set and stops on a repeat. That is not just an optimisation: the
 * writing side rejects cycles ([wouldCreateCycle]), but data that predates it — an imported backup,
 * a hand-edited database — could still contain one, and a naive walk would loop forever rather than
 * merely showing a wrong answer.
 */

/**
 * [tagId] plus every tag that transitively implies it — the set a filter on [tagId] has to match.
 * Filtering by "vegetarisch" returns this as {vegetarisch, vegan, …}, so vegan-only foods turn up
 * too.
 */
fun tagFilterClosure(tagId: String, implications: List<TagImplication>): Set<String> {
    val childrenOf = implications.groupBy({ it.parentTagId }) { it.childTagId }
    return closureFrom(setOf(tagId), childrenOf)
}

/**
 * Every tag [tagIds] transitively implies, walking up the graph, including [tagIds] themselves. What
 * a food would count as if the hierarchy were resolved.
 */
fun impliedTagsClosure(tagIds: Set<String>, implications: List<TagImplication>): Set<String> {
    val parentsOf = implications.groupBy({ it.childTagId }) { it.parentTagId }
    return closureFrom(tagIds, parentsOf)
}

/**
 * True when declaring "[childTagId] implies [parentTagId]" would close a loop — either because the
 * parent already implies the child somewhere up the chain, or because the two are the same tag.
 * Callers use this to grey the option out rather than to report a failure after the fact.
 */
fun wouldCreateCycle(
    childTagId: String,
    parentTagId: String,
    implications: List<TagImplication>,
): Boolean = childTagId == parentTagId ||
    childTagId in impliedTagsClosure(setOf(parentTagId), implications)

/** Breadth-first walk from [start] along [edges], returning everything reached including [start]. */
private fun closureFrom(start: Set<String>, edges: Map<String, List<String>>): Set<String> {
    val visited = start.toMutableSet()
    val queue = ArrayDeque(start)
    while (queue.isNotEmpty()) {
        edges[queue.removeFirst()].orEmpty().forEach { next ->
            if (visited.add(next)) queue.addLast(next)
        }
    }
    return visited
}
