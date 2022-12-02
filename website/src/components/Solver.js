import React from 'react'
import solver from '../js/solver'

const Solver = ({ puzzle, year }) => {
  React.useEffect(() => solver(puzzle, year), [])
  return (
    <div id={puzzle}></div>
  )
}

export default Solver
