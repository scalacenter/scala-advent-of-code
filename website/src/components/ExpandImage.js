import React from 'react'

const ExpandImage = ({ children }) => {

  const StyledChildren = () =>
    React.Children.map(children, child => {
        return (
          <div className='image-container-175mw'>
            {child}
          </div>
        )
      }
    );

  return <StyledChildren />;
}

export default ExpandImage
