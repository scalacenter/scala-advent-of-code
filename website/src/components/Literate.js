import React from 'react'

const Literate = ({ children }) => {
  // see https://stackoverflow.com/a/59431317
  const StyledChildren = () =>
    React.Children.map(children, child =>
      React.cloneElement(child, {
        className: `${child.props.className} ${"literate"}`
      })
    );

  return <StyledChildren />;
}

export default Literate
